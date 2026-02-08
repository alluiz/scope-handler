package com.company.scopehandler.cli;

import com.company.scopehandler.api.config.AppConfig;
import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.ports.AuthorizationServerService;
import com.company.scopehandler.api.services.AuthorizationServerFactory;
import com.company.scopehandler.api.services.AuthorizationServerFactory;
import com.company.scopehandler.api.strategy.ModeStrategy;
import com.company.scopehandler.api.strategy.ModeStrategyFactory;
import com.company.scopehandler.providers.axway.AxwayClientFactory;
import com.company.scopehandler.providers.mock.MockClientFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "scope-handler",
        mixinStandardHelpOptions = true,
        version = "scope-handler 1.0.0",
        description = "Batch de associacao e desassociacao de escopos OAuth 2.0"
)
public final class ScopeBatchApp implements Callable<Integer> {

    @Option(names = "--mode", required = true, description = "Modo: associate|dissociate")
    private String mode;

    @Option(names = "--clients", split = ",", description = "Lista de clients separados por virgula")
    private List<String> clients;

    @Option(names = "--clients-file", description = "Arquivo com um client por linha")
    private Path clientsFile;

    @Option(names = "--scopes", split = ",", description = "Lista de escopos separados por virgula")
    private List<String> scopes;

    @Option(names = "--scopes-file", description = "Arquivo com um escopo por linha")
    private Path scopesFile;

    @Option(names = "--create-scope", description = "Cria o escopo antes de associar (opcional)")
    private boolean createScope;

    @Option(names = "--confirm", description = "Confirma operacao de desassociacao")
    private boolean confirm;

    @Option(names = "--threads", description = "Maximo de threads")
    private Integer threads;

    @Option(names = "--threshold", description = "Limite para habilitar multi-thread")
    private Integer threshold;

    @Option(names = "--audit-dir", description = "Diretorio de auditoria")
    private Path auditDir;

    @Option(names = "--config", description = "Arquivo de propriedades externo")
    private Path configFile;

    @Option(names = "--as", description = "Nome do Authorization Server")
    private String asName;

    @Option(names = "--env", description = "Ambiente do AS (ex: dev, hml, prod)")
    private String environment;

    @Option(names = "--as-base-url", description = "Base URL do AS (override do ambiente)")
    private String asBaseUrl;

    @Option(names = "--user", description = "Usuario de basic auth")
    private String user;

    @Option(names = "--password", description = "Senha de basic auth", interactive = true)
    private String password;

    @Option(names = "--credentials-file", description = "Arquivo de credenciais (formato env). Padrao: ./credentials")
    private Path credentialsFile;

    @Option(names = "--test", description = "Executa em modo teste usando clients.txt e scopes.txt e AS mock")
    private boolean testMode;

    @Option(names = "--debug", description = "Habilita logs detalhados por operacao")
    private boolean debug;

    @Option(names = "--ignore-cache", description = "Ignora cache de execucao anterior")
    private boolean ignoreCache;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ScopeBatchApp()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        if (testMode) {
            applyTestDefaults();
        }

        if (asName == null || asName.isBlank()) {
            throw new IllegalArgumentException("Parametro --as e obrigatorio.");
        }
        if (environment == null || environment.isBlank()) {
            throw new IllegalArgumentException("Parametro --env e obrigatorio.");
        }

        CredentialsService credentialsService = new CredentialsService();
        CredentialsService.Result credentials = credentialsService.resolve(user, password, credentialsFile);
        user = credentials.username();
        password = credentials.password();

        Mode parsedMode = Mode.from(mode);
        if (parsedMode == Mode.DISSOCIATE) {
            new ConfirmationService().confirmDestructiveOperation(confirm);
        }
        if (parsedMode == Mode.DISSOCIATE && createScope) {
            throw new IllegalArgumentException("create-scope nao se aplica ao modo desassociar");
        }

        String resolvedAsName = normalize(asName);
        String resolvedEnv = normalize(environment);

        AppConfig config = new CliConfigService().buildConfig(
                configFile,
                resolvedAsName,
                resolvedEnv,
                asBaseUrl,
                user,
                password,
                auditDir
        );
        InputResolverService inputResolver = new InputResolverService();
        List<String> resolvedClients = inputResolver.resolve(clients, clientsFile);
        List<String> resolvedScopes = inputResolver.resolve(scopes, scopesFile);

        int resolvedThreshold = threshold != null ? threshold : config.getInt("batch.threads.threshold", 500);
        int resolvedThreads = threads != null ? threads : config.getInt("batch.threads.max", 8);
        Path resolvedAuditDir = auditDir != null ? auditDir : config.getPath("audit.dir", "./audit");
        AuthorizationServerFactory registry = new RegistryService(
                new MockClientFactory(),
                new AxwayClientFactory()
        ).build(config, resolvedEnv, resolvedAuditDir.resolve("cache"));
        AuthorizationServerService asClient = registry.create(resolvedAsName);
        ModeStrategy strategy = new ModeStrategyFactory().create(parsedMode, asClient, createScope);

        BatchRunInput input = new BatchRunInput(
                resolvedClients,
                resolvedScopes,
                strategy,
                resolvedAuditDir,
                resolvedThreshold,
                resolvedThreads,
                debug,
                ignoreCache,
                resolvedAsName,
                resolvedEnv
        );
        new BatchRunner().run(input);

        return 0;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase();
    }

    private void applyTestDefaults() {
        if (clientsFile == null) {
            clientsFile = Path.of("clients.txt");
        }
        if (scopesFile == null) {
            scopesFile = Path.of("scopes.txt");
        }
        if (credentialsFile == null) {
            credentialsFile = Path.of("credentials");
        }
        if (asName == null || asName.isBlank()) {
            asName = "mock";
        }
        if (environment == null || environment.isBlank()) {
            environment = "dev";
        }
    }

}
