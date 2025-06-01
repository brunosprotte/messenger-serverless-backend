#!/bin/bash

# Uso: ./deploy-lambda.sh com.messenger.handler.usuarios.UsuarioCreateHandler usuario-create
# Gera o JAR e faz o deploy com Terraform usando módulo

HANDLER="$1"
LAMBDA_NAME="$2"

if [ -z "$HANDLER" ] || [ -z "$LAMBDA_NAME" ]; then
  echo "❌ Uso: $0 <HandlerClass> <LambdaName>"
  exit 1
fi

# Passo 1: Build do JAR
./build-lambda.sh "$HANDLER" "$LAMBDA_NAME"
if [ $? -ne 0 ]; then
  echo "❌ Erro ao construir o JAR da Lambda $LAMBDA_NAME"
  exit 1
fi

cd ../infra || exit 1

# Passo 2: Aplica Terraform no módulo correspondente
MODULE_NAME="${LAMBDA_NAME//-/_}_lambda"

echo "🚀 Fazendo deploy do módulo module.$MODULE_NAME..."
terraform apply -target=module.$MODULE_NAME --auto-approve

cd ..