const jwt = require("jsonwebtoken");

var token = jwt.sign(
  { sub: "usuario123", name: "bruno", email: "bruno@email.com" },
  "7ce86ced-b98f-4ff0-8366-f27b0ffcdc48",
  { issuer: "local-auth0", expiresIn: "1h" }
);

console.log(token);

token = jwt.sign(
  { sub: "usuario123", name: "mococa", email: "mococa@email.com" },
  "7ce86ced-b98f-4ff0-8366-f27b0ffcdc48",
  { issuer: "local-auth0", expiresIn: "1h" }
);

console.log(token);