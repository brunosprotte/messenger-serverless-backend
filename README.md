# Build

Use `./build-lambda.sh com.messenger.handler.usuarios.UsuarioCreateHandler usuario-create` para empacotar as lambdas

```
cd scripts

./build-lambda.sh com.messenger.handler.auth.AuthHandler auth
./build-lambda.sh com.messenger.handler.usuarios.UsuarioCreateHandler usuario-create
./build-lambda.sh com.messenger.handler.usuarios.FotoUploadHandler foto-upload
./build-lambda.sh com.messenger.handler.contatos.ContatoCreateHandler contato-create
./build-lambda.sh com.messenger.handler.contatos.ContatoUpdateHandler contato-update
./build-lambda.sh com.messenger.handler.contatos.ContatoDeleteHandler contato-delete
```

# Build 'n Deploy
```
cd scripts/
./deploy-lambda.sh com.messenger.handler.usuarios.AuthHandler auth
./deploy-lambda.sh com.messenger.handler.usuarios.UsuarioCreateHandler usuario-create
./deploy-lambda.sh com.messenger.handler.usuarios.FotoUploadHandler foto-upload
./deploy-lambda.sh com.messenger.handler.contatos.ContatoCreateHandler contato-create
./deploy-lambda.sh com.messenger.handler.contatos.ContatoUpdateHandler contato-update
./deploy-lambda.sh com.messenger.handler.contatos.ContatoDeleteHandler contato-delete
```

# Terraform
```
cd infra
terraform apply

terraform apply -target=module.auth_lambda --auto-approve
```

