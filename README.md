# Build

Use `./build-lambda.sh com.messenger.handler.usuarios.UsuarioCreateHandler usuario-create` para empacotar as lambdas

```
cd scripts

./build-lambda.sh com.messenger.handler.usuarios.UsuarioCreateHandler usuario-create
./build-lambda.sh com.messenger.handler.contatos.ContatoCreateHandler contato-create
./build-lambda.sh com.messenger.handler.auth.AuthHandler auth
```

# Build 'n Deploy
```
cd scripts/
./deploy-lambda.sh com.messenger.handler.usuarios.UsuarioCreateHandler usuario-create
./deploy-lambda.sh com.messenger.handler.contatos.ContatoCreateHandler contato-create
./deploy-lambda.sh com.messenger.handler.usuarios.AuthHandler auth
```

# Terraform
```
cd infra
terraform apply

terraform apply -target=module.auth_lambda --auto-approve
```

