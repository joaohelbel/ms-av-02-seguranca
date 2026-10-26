-- Usuários do "sistema genérico" (validados pelo POST /login)
INSERT INTO usuarios (username, senha) VALUES ('joao', '123456');
INSERT INTO usuarios (username, senha) VALUES ('bianca', '123456');
INSERT INTO usuarios (username, senha) VALUES ('maria', 'abc123');
INSERT INTO usuarios (username, senha) VALUES ('pedro', 'abc123');

INSERT INTO login_attempts (username) VALUES ('joao');
INSERT INTO login_attempts (username) VALUES ('bianca');
INSERT INTO login_attempts (username) VALUES ('maria');
INSERT INTO login_attempts (username) VALUES ('pedro');

-- Os usuários com role (admin / manutencao) são criados na subida da
-- aplicação pelo JdbcUserDetailsManager (ver CargaUsuariosRoles.java),
-- assim a senha já entra criptografada em BCrypt.
