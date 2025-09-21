# 📋 Guia dos Makefiles - Banking Demo

Este projeto utiliza Makefiles para automatizar tarefas de desenvolvimento, teste e deploy. Existem dois Makefiles principais:

## 📁 Estrutura dos Makefiles

### `Makefile` - Principal
Comandos para uso geral, produção e CI/CD.

### `Makefile.dev` - Desenvolvimento  
Comandos específicos para desenvolvimento local e debug.

## 🚀 Comandos por Categoria

### 📦 Setup e Inicialização

| Comando | Descrição | Quando Usar |
|---------|-----------|-------------|
| `make setup` | Setup completo do projeto | **Primeira vez** ou reset completo |
| `make setup-infra` | Inicia infraestrutura Docker | Quando containers param |
| `make setup-cdc` | Configura CDC (Debezium) | Problemas com CDC |
| `make check-infra` | Verifica infraestrutura | Validar se tudo está funcionando |

**Exemplo de uso:**
```bash
# Primeira vez no projeto
make setup

# Apenas reiniciar containers
make setup-infra
```

### 🔨 Build e Deploy

| Comando | Descrição | Quando Usar |
|---------|-----------|-------------|
| `make build-all` | Compila todos os serviços | Após mudanças no código |
| `make build-service SERVICE=nome` | Compila serviço específico | Mudança em um serviço |
| `make run-service SERVICE=nome` | Executa um serviço | Testar serviço individual |

**Exemplo de uso:**
```bash
# Compilar tudo
make build-all

# Compilar apenas account-service
make build-service SERVICE=account-service

# Executar transfer-service
make run-service SERVICE=transfer-service
```

### 🧪 Testes

| Comando | Descrição | Quando Usar |
|---------|-----------|-------------|
| `make test-all` | Executa todos os testes | CI/CD ou validação completa |
| `make test-transfer` | Testa transferência | Validar fluxo principal |
| `make test-cdc` | Testa CDC completo | Problemas com auditoria |
| `make test-create-account` | Cria conta de teste | Setup de dados de teste |

**Exemplo de uso:**
```bash
# Teste completo
make test-all

# Teste rápido de transferência
make test-transfer
```

### 🔍 Debug e Diagnóstico

| Comando | Descrição | Quando Usar |
|---------|-----------|-------------|
| `make debug-all` | Diagnóstico completo | Problemas gerais |
| `make debug-cdc` | Debug do CDC | CDC não funciona |
| `make debug-temporal` | Debug do Temporal | Workflows com erro |
| `make debug-services` | Status dos microserviços | Verificar saúde dos serviços |

**Exemplo de uso:**
```bash
# Diagnóstico geral
make debug-all

# Problema específico com CDC
make debug-cdc
```

### 🛠️ Correções

| Comando | Descrição | Quando Usar |
|---------|-----------|-------------|
| `make fix-audit-db` | Corrige tabela JSONB | Erro de tipo no audit-service |
| `make reset-cdc` | Reset completo do CDC | CDC travado ou corrompido |
| `make reset-temporal` | Reset workflows | Workflows em loop ou travados |
| `make reset-audit-consumer` | Reset consumer auditoria | Consumer travado ou com offset errado |

**Exemplo de uso:**
```bash
# Erro JSONB no audit
make fix-audit-db

# CDC não está funcionando
make reset-cdc
```

### 🧹 Limpeza

| Comando | Descrição | Quando Usar |
|---------|-----------|-------------|
| `make clean` | Limpeza completa | Reset total do ambiente |
| `make stop` | Para todos os serviços | Finalizar trabalho |
| `make stop-services` | Para apenas microserviços | Manter infraestrutura |

## 🛠️ Comandos de Desenvolvimento (Makefile.dev)

### 🚀 Desenvolvimento Rápido

| Comando | Descrição | Quando Usar |
|---------|-----------|-------------|
| `make -f Makefile.dev dev-setup` | Setup de desenvolvimento | Ambiente de dev |
| `make -f Makefile.dev dev-start` | Inicia todos em background | Desenvolvimento ativo |
| `make -f Makefile.dev dev-stop` | Para todos os serviços | Fim do desenvolvimento |
| `make -f Makefile.dev dev-restart SERVICE=nome` | Reinicia um serviço | Após mudança no código |

**Exemplo de uso:**
```bash
# Setup inicial de dev
make -f Makefile.dev dev-setup

# Iniciar todos os serviços
make -f Makefile.dev dev-start

# Reiniciar após mudança
make -f Makefile.dev dev-restart SERVICE=account-service
```

### 📋 Logs e Monitoramento

| Comando | Descrição | Quando Usar |
|---------|-----------|-------------|
| `make -f Makefile.dev dev-logs SERVICE=nome` | Logs de um serviço | Debug específico |
| `make -f Makefile.dev dev-logs-all` | Logs de todos | Visão geral |
| `make -f Makefile.dev dev-monitor` | Monitor em tempo real | Acompanhar sistema |

**Exemplo de uso:**
```bash
# Ver logs do account-service
make -f Makefile.dev dev-logs SERVICE=account-service

# Monitor geral
make -f Makefile.dev dev-monitor
```

### 🧪 Testes de Desenvolvimento

| Comando | Descrição | Quando Usar |
|---------|-----------|-------------|
| `make -f Makefile.dev dev-test-flow` | Teste completo do fluxo | Validação end-to-end |
| `make -f Makefile.dev dev-test-cdc-only` | Teste apenas CDC | Debug de auditoria |
| `make -f Makefile.dev dev-test-temporal` | Teste apenas Temporal | Debug de workflows |

### 🔍 Debug Avançado

| Comando | Descrição | Quando Usar |
|---------|-----------|-------------|
| `make -f Makefile.dev dev-debug-kafka` | Debug detalhado Kafka | Problemas com mensagens |
| `make -f Makefile.dev dev-debug-db` | Debug dos bancos | Verificar dados |

## 🎯 Fluxos de Trabalho Recomendados

### 🆕 Primeira Vez no Projeto
```bash
make setup
make -f Makefile.dev dev-start
make test-transfer
```

### 💻 Desenvolvimento Diário
```bash
# Iniciar dia
make -f Makefile.dev dev-start

# Após mudança no código
make build-service SERVICE=account-service
make -f Makefile.dev dev-restart SERVICE=account-service

# Testar mudança
make -f Makefile.dev dev-test-flow

# Fim do dia
make -f Makefile.dev dev-stop
```

### 🐛 Debug de Problemas
```bash
# Diagnóstico geral
make debug-all

# Problema específico com CDC
make debug-cdc
make reset-cdc

# Problema com Temporal
make debug-temporal
make reset-temporal

# Problema com banco
make fix-audit-db
```

### 🧪 Antes de Commit
```bash
make test-all
make debug-all
```

### 🚀 Deploy/Produção
```bash
make clean
make setup
make test-all
```

## 💡 Dicas e Truques

### ⚡ Comandos Rápidos
```bash
# Ver todos os comandos
make help

# URLs do sistema
make urls

# Status rápido
make debug-services
```

### 🔧 Personalização
Você pode criar seus próprios targets no Makefile:

```makefile
my-custom-test: ## 🧪 Meu teste personalizado
	@echo "Executando meu teste..."
	@make test-cdc
	@make test-transfer
```

### 📊 Monitoramento Contínuo
```bash
# Terminal 1: Monitor
make -f Makefile.dev dev-monitor

# Terminal 2: Logs
make -f Makefile.dev dev-logs-all

# Terminal 3: Desenvolvimento
make -f Makefile.dev dev-restart SERVICE=account-service
```

## 🆘 Troubleshooting

### ❌ Comando não encontrado
```bash
# Verificar se Make está instalado
which make

# No macOS
brew install make

# No Ubuntu/Debian
sudo apt-get install make
```

### ❌ Permissão negada
```bash
# Dar permissão aos scripts
chmod +x scripts/**/*.sh
```

### ❌ Serviço não inicia
```bash
# Debug específico
make debug-services
make -f Makefile.dev dev-logs SERVICE=nome-do-servico
```

### ❌ CDC não funciona
```bash
# Reset completo
make reset-cdc
make fix-audit-db
```

## 📚 Referências

- [GNU Make Manual](https://www.gnu.org/software/make/manual/)
- [Makefile Tutorial](https://makefiletutorial.com/)
- Documentação específica do projeto no README.md