#!/bin/bash

# Uso: ./build-lambda.sh <HandlerClass> <LambdaName>
HANDLER="$1"
LAMBDA_NAME="$2"

if [ -z "$HANDLER" ] || [ -z "$LAMBDA_NAME" ]; then
  echo "❌ Uso: $0 <HandlerClass> <LambdaName>"
  exit 1
fi

echo "📦 Empacotando Lambda:"
echo "🔹 Handler: $HANDLER"
echo "🔹 Nome da Lambda: $LAMBDA_NAME"

cd ..

mkdir -p builds

# Passa o nome da lambda como variável de ambiente para o Maven
LAMBDA_NAME="$LAMBDA_NAME" mvn clean package -Dmain.class="$HANDLER"

# Captura o JAR com dependências gerado pelo Maven Shade
JAR_COM_DEP=$(find target -type f -name "${LAMBDA_NAME}-shaded.jar" | head -n 1)

if [ -f "$JAR_COM_DEP" ]; then
  cp "$JAR_COM_DEP" "builds/${LAMBDA_NAME}.jar"
  echo "✅ JAR final: builds/${LAMBDA_NAME}.jar"
else
  echo "❌ Não foi possível localizar o JAR com dependências"
  exit 2
fi