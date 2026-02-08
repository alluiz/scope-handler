package com.company.scopehandler.cli;

import com.company.scopehandler.api.config.AppConfig;
import com.company.scopehandler.cli.config.ConfigLoader;

import java.nio.file.Path;
import java.util.Properties;

public final class CliConfigService {
    public AppConfig buildConfig(Path configFile,
                                 String asName,
                                 String environment,
                                 String asBaseUrl,
                                 String user,
                                 String password,
                                 Path auditDir) {
        Properties props = ConfigLoader.load(configFile);
        AppConfig.Builder builder = AppConfig.builder().fromProperties(props);

        if (asBaseUrl != null) {
            builder.set("as." + asName + ".env." + environment + ".baseUrl", asBaseUrl);
        }
        if (user != null) {
            builder.set("as." + asName + ".auth.username", user);
        }
        if (password != null) {
            builder.set("as." + asName + ".auth.password", password);
        }
        if (auditDir != null) {
            builder.set("audit.dir", auditDir.toString());
        }

        return builder.build();
    }
}
