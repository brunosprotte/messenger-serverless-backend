module "auth_lambda" {
  source       = "./modules/lambda"
  lambda_name  = "auth"
  lambda_file  = "auth.jar"
  handler      = "com.messenger.handler.auth.AuthHandler"
  role_arn     = aws_iam_role.lambda_exec.arn
}

module "usuario_create_lambda" {
  source       = "./modules/lambda"
  lambda_name  = "usuario-create"
  lambda_file  = "usuario-create.jar"
  handler      = "com.messenger.handler.usuarios.UsuarioCreateHandler::handleRequest"
  role_arn     = aws_iam_role.lambda_exec.arn
}