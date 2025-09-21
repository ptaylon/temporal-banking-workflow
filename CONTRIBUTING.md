# Contribuindo para o Banking Demo

Obrigado por considerar contribuir para o Banking Demo! Este documento fornece diretrizes para contribuições.

## 🚀 Como Contribuir

### 1. Fork e Clone

```bash
# Fork o repositório no GitHub
# Clone seu fork
git clone https://github.com/SEU_USERNAME/banking-demo.git
cd banking-demo

# Adicione o repositório original como upstream
git remote add upstream https://github.com/ORIGINAL_OWNER/banking-demo.git
```

### 2. Configurar Ambiente de Desenvolvimento

```bash
# Inicie a infraestrutura
./scripts/start-banking-demo.sh

# Execute os testes
./mvnw test
```

### 3. Criar Branch para Feature

```bash
# Crie uma branch descritiva
git checkout -b feature/nova-funcionalidade
# ou
git checkout -b fix/correcao-bug
# ou
git checkout -b docs/melhoria-documentacao
```

## 📋 Diretrizes de Desenvolvimento

### Padrões de Código

- **Java**: Siga as convenções do Google Java Style Guide
- **Nomenclatura**: Use nomes descritivos em inglês para código, português para documentação
- **Comentários**: Documente métodos públicos e lógica complexa
- **Testes**: Mantenha cobertura de testes acima de 80%

### Estrutura de Commits

Use o padrão Conventional Commits:

```
tipo(escopo): descrição

feat(account): adiciona validação de CPF
fix(transfer): corrige cálculo de taxa
docs(readme): atualiza instruções de instalação
test(audit): adiciona testes de integração CDC
refactor(validation): melhora performance da validação
```

### Tipos de Commit

- `feat`: Nova funcionalidade
- `fix`: Correção de bug
- `docs`: Documentação
- `style`: Formatação (sem mudança de lógica)
- `refactor`: Refatoração de código
- `test`: Adição ou correção de testes
- `chore`: Tarefas de manutenção

## 🧪 Testes

### Executar Testes

```bash
# Todos os testes
./mvnw test

# Testes de um serviço específico
./mvnw test -pl account-service

# Testes de integração
./mvnw verify

# Teste do CDC
./scripts/test-audit-cdc.sh
```

### Escrever Testes

- **Unitários**: Para lógica de negócio
- **Integração**: Para workflows Temporal
- **CDC**: Para auditoria e eventos
- **API**: Para endpoints REST

Exemplo de teste unitário:

```java
@Test
void shouldValidateTransferAmount() {
    // Given
    TransferRequest request = new TransferRequest("123", "456", new BigDecimal("100.00"), "BRL");
    
    // When
    ValidationResult result = validationService.validate(request);
    
    // Then
    assertThat(result.isValid()).isTrue();
}
```

## 📚 Documentação

### Atualizar Documentação

- README.md para mudanças gerais
- JavaDoc para métodos públicos
- Comentários inline para lógica complexa
- Scripts de exemplo para novas APIs

### Formato da Documentação

- Use Markdown para documentos
- Inclua exemplos de código
- Adicione diagramas quando necessário
- Mantenha instruções atualizadas

## 🔍 Code Review

### Antes de Submeter PR

```bash
# Sincronize com upstream
git fetch upstream
git rebase upstream/main

# Execute testes
./mvnw clean test

# Verifique formatação
./mvnw spotless:check

# Execute diagnóstico
./scripts/diagnose-cdc.sh
```

### Checklist do PR

- [ ] Testes passando
- [ ] Documentação atualizada
- [ ] Código formatado
- [ ] Commits seguem padrão
- [ ] Sem conflitos com main
- [ ] Funcionalidade testada manualmente

## 🐛 Reportar Bugs

### Template de Bug Report

```markdown
**Descrição do Bug**
Descrição clara e concisa do problema.

**Passos para Reproduzir**
1. Execute '...'
2. Clique em '...'
3. Veja o erro

**Comportamento Esperado**
O que deveria acontecer.

**Screenshots**
Se aplicável, adicione screenshots.

**Ambiente**
- OS: [e.g. macOS, Ubuntu]
- Java: [e.g. 21]
- Docker: [e.g. 24.0.0]

**Logs**
```
Cole logs relevantes aqui
```

## 💡 Sugerir Funcionalidades

### Template de Feature Request

```markdown
**Funcionalidade Desejada**
Descrição clara da funcionalidade.

**Problema que Resolve**
Que problema esta funcionalidade resolve?

**Solução Proposta**
Como você imagina que deveria funcionar?

**Alternativas Consideradas**
Outras soluções que você considerou?

**Contexto Adicional**
Qualquer outro contexto sobre a funcionalidade.
```

## 🏗️ Arquitetura

### Princípios

- **Microserviços**: Cada serviço tem responsabilidade única
- **Event-Driven**: Comunicação via eventos Kafka
- **CQRS**: Separação de comando e consulta
- **Saga Pattern**: Transações distribuídas
- **CDC**: Auditoria automática

### Adicionando Novo Serviço

1. Crie módulo Maven
2. Configure Spring Boot
3. Adicione ao docker-compose
4. Implemente health checks
5. Adicione testes
6. Atualize documentação

## 📞 Suporte

- **Issues**: Para bugs e features
- **Discussions**: Para dúvidas gerais
- **Email**: Para questões sensíveis

## 📄 Licença

Ao contribuir, você concorda que suas contribuições serão licenciadas sob a mesma licença do projeto (MIT).

---

Obrigado por contribuir! 🎉