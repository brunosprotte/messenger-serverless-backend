module "usuario_create_lambda" {
  source      = "./modules/lambda"
  lambda_name = "usuario-create"
  lambda_file = "usuario-create.jar"
  handler     = "com.messenger.handler.usuarios.UsuarioCreateHandler::handleRequest"
  role_arn    = aws_iam_role.lambda_exec.arn
}

module "usuario_update_lambda" {
  source      = "./modules/lambda"
  lambda_name = "usuario-update"
  lambda_file = "usuario-update.jar"
  handler     = "com.messenger.handler.usuarios.UsuarioUpdateHandler::handleRequest"
  role_arn    = aws_iam_role.lambda_exec.arn
}

module "foto_upload_lambda" {
  source      = "./modules/lambda"
  lambda_name = "foto-upload"
  lambda_file = "foto-upload.jar"        # Ajuste para o seu JAR
  handler = "com.messenger.handler.usuarios.FotoUploadHandler::handleRequest"  # Ajuste para o seu handler
  role_arn    = aws_iam_role.lambda_exec.arn
}

module "contato_create_lambda" {
  source      = "./modules/lambda"
  lambda_name = "contato-create"
  lambda_file = "contato-create.jar"
  handler     = "com.messenger.handler.contatos.ContatoCreateHandler::handleRequest"
  role_arn    = aws_iam_role.lambda_exec.arn
}

module "contato_list_lambda" {
  source      = "./modules/lambda"
  lambda_name = "contato-list"
  lambda_file = "contato-list.jar"
  handler     = "com.messenger.handler.contatos.ContatosListHandler::handleRequest"
  role_arn    = aws_iam_role.lambda_exec.arn
}

module "contato_update_lambda" {
  source      = "./modules/lambda"
  lambda_name = "contato-update"
  lambda_file = "contato-update.jar"
  handler     = "com.messenger.handler.contatos.ContatoUpdateHandler::handleRequest"
  role_arn    = aws_iam_role.lambda_exec.arn
}

module "contato_delete_lambda" {
  source      = "./modules/lambda"
  lambda_name = "contato-delete"
  lambda_file = "contato-delete.jar"
  handler     = "com.messenger.handler.contatos.ContatoDeleteHandler::handleRequest"
  role_arn    = aws_iam_role.lambda_exec.arn
}