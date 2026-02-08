package com.company.scopehandler.app;

import com.company.scopehandler.config.AppConfig;
import com.company.scopehandler.config.ConfigLoader;
import com.company.scopehandler.config.Credentials;
import com.company.scopehandler.config.CredentialsLoader;
import com.company.scopehandler.domain.Mode;
import com.company.scopehandler.ports.AuthorizationServerClient;
import com.company.scopehandler.services.*;
import com.company.scopehandler.strategy.ModeStrategy;
import com.company.scopehandler.strategy.ModeStrategyFactory;
import com.company.scopehandler.usecases.ExecuteBatchUseCase;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
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

        resolveCredentials();

        Mode parsedMode = Mode.from(mode);
        if (parsedMode == Mode.DISSOCIATE) {
            new ConfirmationService().confirmDestructiveOperation(confirm);
        }
        if (parsedMode == Mode.DISSOCIATE && createScope) {
            throw new IllegalArgumentException("create-scope nao se aplica ao modo desassociar");
        }

        String resolvedAsName = normalize(asName);
        String resolvedEnv = normalize(environment);

        AppConfig config = buildConfig(resolvedAsName, resolvedEnv);
        InputResolverService inputResolver = new InputResolverService();
        List<String> resolvedClients = inputResolver.resolve(clients, clientsFile);
        List<String> resolvedScopes = inputResolver.resolve(scopes, scopesFile);

        int resolvedThreshold = threshold != null ? threshold : config.getInt("batch.threads.threshold", 500);
        int resolvedThreads = threads != null ? threads : config.getInt("batch.threads.max", 8);
        Path resolvedAuditDir = auditDir != null ? auditDir : config.getPath("audit.dir", "./audit");
        AuthorizationServerClient asClient = new AuthorizationServerFactory().create(resolvedAsName, resolvedEnv, config, resolvedAuditDir.resolve("cache"));
        ModeStrategy strategy = new ModeStrategyFactory().create(parsedMode, asClient, createScope);

        BatchPlannerService plannerService = new BatchPlannerService();
        BatchExecutorService executorService = new BatchExecutorService();
        ExecuteBatchUseCase useCase = new ExecuteBatchUseCase(plannerService, executorService);

        java.nio.file.Path cacheDir = resolvedAuditDir.resolve("cache");
        java.nio.file.Path cacheFile = cacheDir.resolve("resume-cache-" + asName + "-" + environment + ".txt");
        com.company.scopehandler.cache.ExecutionCache cache = com.company.scopehandler.cache.ExecutionCache.load(cacheFile, !ignoreCache);

        boolean completed = false;
        try (AuditService auditService = new AuditService(resolvedAuditDir);
             com.company.scopehandler.cache.ExecutionCache ignored = cache) {
            BatchReport report = useCase.execute(
                    resolvedClients,
                    resolvedScopes,
                    strategy,
                    auditService,
                    resolvedThreshold,
                    resolvedThreads,
                    debug,
                    cache
            );
            Path reportPath = new ReportService().writeReport(resolvedAuditDir, report);

            printSummary(report, auditService.getFilePath(), reportPath, resolvedThreshold, resolvedThreads);
            completed = true;
        } finally {
            if (completed && !ignoreCache) {
                cache.deleteFile();
            }
        }

        return 0;
    }

    private AppConfig buildConfig(String asName, String environment) {
        Properties props = ConfigLoader.load(configFile);

        if (asBaseUrl != null) {
            props.setProperty("as." + asName + ".env." + environment + ".baseUrl", asBaseUrl);
        }
        if (user != null) {
            props.setProperty("as." + asName + ".auth.username", user);
        }
        if (password != null) {
            props.setProperty("as." + asName + ".auth.password", password);
        }
        if (auditDir != null) {
            props.setProperty("audit.dir", auditDir.toString());
        }

        return new AppConfig(props);
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

    private void resolveCredentials() {
        if (user != null && !user.isBlank() && password != null && !password.isBlank()) {
            return;
        }

        Path file = credentialsFile != null ? credentialsFile : Path.of("credentials");
        Credentials creds = CredentialsLoader.load(file);
        if (user == null || user.isBlank()) {
            user = creds.username();
        }
        if (password == null || password.isBlank()) {
            password = creds.password();
        }

        if (user == null || user.isBlank() || password == null || password.isBlank()) {
            promptForCredentials();
        }
    }

    private void promptForCredentials() {
        java.io.Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Nao foi possivel ler credenciais. Informe --user/--password ou arquivo.");
        }
        if (user == null || user.isBlank()) {
            user = console.readLine("Usuario: ");
        }
        if (password == null || password.isBlank()) {
            char[] secret = console.readPassword("Senha: ");
            if (secret != null) {
                password = new String(secret);
            }
        }
    }

    private void printSummary(BatchReport report, Path auditFile, Path reportFile, int threshold, int threads) {
        System.out.println("Batch concluido");
        System.out.println("Total: " + report.getTotal());
        System.out.println("Success: " + report.getSuccessCount());
        System.out.println("Failure: " + report.getFailureCount());
        System.out.println("Skipped: " + report.getSkipCount());
        System.out.println("Duracao total: " + com.company.scopehandler.services.DurationFormatter.formatSeconds(report.getDurationSeconds()));
        System.out.println("Media por operacao: " + String.format(java.util.Locale.ROOT, "%.2f", report.getAverageMsPerOperation()) + "ms");
        System.out.println("Audit: " + auditFile);
        System.out.println("Report: " + reportFile);
        System.out.println("Multi-thread: threshold=" + threshold + " maxThreads=" + threads);
        if (!report.getSampleErrors().isEmpty()) {
            System.out.println("Amostra de erros:");
            for (String err : report.getSampleErrors()) {
                System.out.println("- " + err);
            }
        }
    }
}
