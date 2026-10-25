const crypto = require('crypto');

function decodeB64(str) {
    return JSON.parse(Buffer.from(str, 'base64url').toString('utf8'));
}

function reject(r, reason) {
    r.warn('JWT rejected: ' + reason);
    r.return(401);
}

function verify_jwt(r) {
    // hard failed when JWT_SECRET is not set
    const secret = process.env.JWT_SECRET;
    if (!secret) {
        r.error('JWT_SECRET is not set, add "env JWT_SECRET;" to nginx.conf');
        r.return(500);
        return;
    }

    const authHeader = r.headersIn['Authorization'];
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return reject(r, 'missing or malformed Authorization header');
    }

    const token = authHeader.split(" ")[1];
    const parts = token.split(".");
    if (parts.length !== 3) {
        return reject(r, 'invalid JWT structure');
    }
    const headerB64 = parts[0];
    const payloadB64 = parts[1];
    const signatureB64 = parts[2];

    // signature check
    const expectedSignature = crypto.createHmac('sha256', secret)
        .update(headerB64 + '.' + payloadB64)
        .digest('base64url');
    if (expectedSignature !== signatureB64) {
        return reject(r, 'invalid signature');
    }

    let header;
    let payload;
    try {
        header = decodeB64(headerB64);
        payload = decodeB64(payloadB64);
    } catch (e) {
        return reject(r, 'cannot decode token: ' + e.message);
    }

    // require alg check
    if (header.alg !== 'HS256') {
        return reject(r, 'unexpected alg ' + header.alg);
    }
    // expiration check
    if (!payload.exp || Math.floor(Date.now() / 1000) >= payload.exp) {
        return reject(r, 'token expired or has no exp');
    }

    // no user id (sub) checl
    if (!payload.sub) {
        return reject(r, 'token has no sub');
    }

    r.headersOut['X-User-Id'] = String(payload.sub);
    r.return(200);
}

export default { verify_jwt };
