terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.66"
    }
  }
}

provider "aws" {
  region                      = "us-east-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  s3_use_path_style           = true

  endpoints {
    lambda     = "http://localhost:4566"
    apigateway = "http://localhost:4566"
    iam        = "http://localhost:4566"
    dynamodb   = "http://localhost:4566"
    s3         = "http://localhost:4566"
  }
}

// IAM Role (compartilhada pelas Lambdas)
resource "aws_iam_role" "lambda_exec" {
  name = "lambda_exec_role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Effect = "Allow"
      }
    ]
  })
}

// API Gateway
resource "aws_api_gateway_rest_api" "api" {
  name        = "MessengerAPI"
  description = "API REST com Lambda via LocalStack"
}

resource "aws_api_gateway_resource" "usuarios_resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "usuarios"
}

resource "aws_api_gateway_resource" "fotos_resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "fotos"
}

resource "aws_api_gateway_resource" "contatos_resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "contatos"
}

resource "aws_api_gateway_method" "usuarios_post_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.usuarios_resource.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "usuarios_patch_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.usuarios_resource.id
  http_method   = "PATCH"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "fotos_post_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.fotos_resource.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "contatos_post_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.contatos_resource.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "contatos_get_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.contatos_resource.id
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "contatos_patch_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.contatos_resource.id
  http_method   = "PATCH"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "contatos_delete_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.contatos_resource.id
  http_method   = "DELETE"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "usuarios_lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.usuarios_resource.id
  http_method             = aws_api_gateway_method.usuarios_post_method.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = module.usuario_create_lambda.invoke_arn
}

resource "aws_api_gateway_integration" "usuarios_patch_lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.usuarios_resource.id
  http_method             = aws_api_gateway_method.usuarios_patch_method.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = module.usuario_update_lambda.invoke_arn
}

resource "aws_api_gateway_integration" "fotos_lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.fotos_resource.id
  http_method             = aws_api_gateway_method.fotos_post_method.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = module.foto_upload_lambda.invoke_arn
}

resource "aws_api_gateway_integration" "contatos_lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.contatos_resource.id
  http_method             = aws_api_gateway_method.contatos_post_method.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = module.contato_create_lambda.invoke_arn
}

resource "aws_api_gateway_integration" "contatos_list_lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.contatos_resource.id
  http_method             = aws_api_gateway_method.contatos_get_method.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = module.contato_list_lambda.invoke_arn
}

resource "aws_api_gateway_integration" "contatos_patch_lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.contatos_resource.id
  http_method             = aws_api_gateway_method.contatos_patch_method.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = module.contato_update_lambda.invoke_arn
}

resource "aws_api_gateway_integration" "contatos_delete_lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.contatos_resource.id
  http_method             = aws_api_gateway_method.contatos_delete_method.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = module.contato_delete_lambda.invoke_arn
}

resource "aws_lambda_permission" "usuarios_apigw_lambda" {
  statement_id  = "AllowAPIGatewayInvokeUsuarios"
  action        = "lambda:InvokeFunction"
  function_name = module.usuario_create_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "usuarios_patch_apigw_lambda" {
  statement_id  = "AllowAPIGatewayInvokeUsuarios"
  action        = "lambda:InvokeFunction"
  function_name = module.usuario_update_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "fotos_apigw_lambda" {
  statement_id  = "AllowAPIGatewayInvokeFotos"
  action        = "lambda:InvokeFunction"
  function_name = module.foto_upload_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "contatos_apigw_lambda" {
  statement_id  = "AllowAPIGatewayInvokeContatos"
  action        = "lambda:InvokeFunction"
  function_name = module.contato_create_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "contatos_get_apigw_lambda" {
  statement_id  = "AllowAPIGatewayInvokeContatosGet"
  action        = "lambda:InvokeFunction"
  function_name = module.contato_list_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "contatos_patch_apigw_lambda" {
  statement_id  = "AllowAPIGatewayInvokeContatos"
  action        = "lambda:InvokeFunction"
  function_name = module.contato_update_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "contatos_delete_apigw_lambda" {
  statement_id  = "AllowAPIGatewayInvokeContatos"
  action        = "lambda:InvokeFunction"
  function_name = module.contato_delete_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_api_gateway_deployment" "deployment" {
  depends_on = [
    aws_api_gateway_integration.usuarios_lambda_integration,
    aws_api_gateway_integration.usuarios_patch_lambda_integration,
    aws_api_gateway_integration.fotos_lambda_integration,
    aws_api_gateway_integration.contatos_lambda_integration,
    aws_api_gateway_integration.contatos_list_lambda_integration,
    aws_api_gateway_integration.contatos_patch_lambda_integration,
    aws_api_gateway_integration.contatos_delete_lambda_integration
  ]
  rest_api_id = aws_api_gateway_rest_api.api.id
  stage_name  = "dev"
}

// DynamoDB

resource "aws_iam_policy" "dynamodb_access" {
  name        = "dynamodb_access"
  description = "Permissão para a Lambda acessar a tabela usuarios"

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Scan",
          "dynamodb:Query"
        ],
        Effect   = "Allow",
        Resource = [
          aws_dynamodb_table.usuarios.arn,
          aws_dynamodb_table.contatos.arn
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_dynamodb" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = aws_iam_policy.dynamodb_access.arn
}

resource "aws_dynamodb_table" "usuarios" {
  name         = "usuarios"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "email"
    type = "S"
  }

  global_secondary_index {
    name               = "email-index"
    hash_key           = "email"
    projection_type    = "ALL"
  }
}

resource "aws_dynamodb_table" "contatos" {
  name         = "contatos"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "usuario_email"
  range_key    = "contato_email"

  attribute {
    name = "usuario_email"
    type = "S"
  }

  attribute {
    name = "contato_email"
    type = "S"
  }
}

resource "aws_dynamodb_table" "mensagens_pendentes" {
  name         = "mensagens_pendentes"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "destinatario_email"
  range_key    = "timestamp"

  attribute {
    name = "destinatario_email"
    type = "S"
  }

  attribute {
    name = "timestamp"
    type = "S"
  }
}

// S3 Bucket

resource "aws_iam_policy" "s3_access" {
  name = "s3_access_policy"

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject"
        ],
        Effect   = "Allow",
        Resource = "arn:aws:s3:::fotos-perfil/*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_s3" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = aws_iam_policy.s3_access.arn
}

resource "aws_s3_bucket" "fotos_perfil" {
  bucket = "fotos-perfil"
}