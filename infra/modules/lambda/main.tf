resource "aws_lambda_function" "lambda" {
  function_name = var.lambda_name
  handler       = var.handler
  runtime       = "java17"
  memory_size   = 512
  timeout       = 10
  role          = var.role_arn

  filename         = "${path.module}/../../../builds/${var.lambda_file}"
  source_code_hash = filebase64sha256("${path.module}/../../../builds/${var.lambda_file}")
}