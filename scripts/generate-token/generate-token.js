const jwt = require("jsonwebtoken");

const token = jwt.sign(
  { sub: "usuario123", name: "João" },
  "7ce86ced-b98f-4ff0-8366-f27b0ffcdc48",
  { issuer: "local-auth0", expiresIn: "1h" }
);

console.log(token);