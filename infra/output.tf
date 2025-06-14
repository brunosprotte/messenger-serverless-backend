output "usuarios_post_url" {
  value = "http://localhost:4566/_aws/execute-api/${aws_api_gateway_rest_api.api.id}/dev/usuarios"
}

output "usuarios_patch_url" {
  value = "http://localhost:4566/_aws/execute-api/${aws_api_gateway_rest_api.api.id}/dev/usuarios"
}

output "fotos_post_url" {
  value = "http://localhost:4566/_aws/execute-api/${aws_api_gateway_rest_api.api.id}/dev/fotos"
}

output "contatos_post_url" {
  value = "http://localhost:4566/_aws/execute-api/${aws_api_gateway_rest_api.api.id}/dev/contatos"
}

output "contatos_list_url" {
  value = "http://localhost:4566/_aws/execute-api/${aws_api_gateway_rest_api.api.id}/dev/contatos"
}

output "contatos_patch_url" {
  value = "http://localhost:4566/_aws/execute-api/${aws_api_gateway_rest_api.api.id}/dev/contatos"
}

output "contatos_delete_url" {
  value = "http://localhost:4566/_aws/execute-api/${aws_api_gateway_rest_api.api.id}/dev/contatos"
}