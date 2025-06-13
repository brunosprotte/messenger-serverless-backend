package com.messenger.shared.model;

public class Usuario {
    private String id;
    private String nome;
    private String senha;
    private String email;
    private String fotoPerfil;

    public Usuario() {}

    public Usuario(String id, String nome, String senha, String email, String fotoPerfil) {
        this.id = id;
        this.nome = nome;
        this.senha = senha;
        this.email = email;
        this.fotoPerfil = fotoPerfil;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }
}
