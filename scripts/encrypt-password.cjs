const CryptoJS = require('../frontend/node_modules/crypto-js');

const key = CryptoJS.enc.Utf8.parse('thanks,pig4cloud');
const password = process.argv[2];

if (!password) {
	process.stderr.write('password argument is required');
	process.exit(1);
}

process.stdout.write(CryptoJS.AES.encrypt(password, key, {
	iv: key,
	mode: CryptoJS.mode.CFB,
	padding: CryptoJS.pad.NoPadding,
}).toString());
