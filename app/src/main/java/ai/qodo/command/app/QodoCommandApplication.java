/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.app;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "ai.qodo.command.app",      // Scan app module
    "ai.qodo.command.internal"  // Scan internal-core module
})
public class QodoCommandApplication {
    public static void main(String[] args) {
        // Print banner immediately before any Spring initialization
        printBannerEarly();
        
        // Create SpringApplication and disable the default banner since we already printed it
        SpringApplication app = new SpringApplication(QodoCommandApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
    
    private static void printBannerEarly() {
        String banner = """
                ·····································································
                :  ██████╗ ██████╗ ███╗   ███╗███╗   ███╗ █████╗ ███╗   ██╗██████╗  :
                : ██╔════╝██╔═══██╗████╗ ████║████╗ ████║██╔══██╗████╗  ██║██╔══██╗ :
                : ██║     ██║   ██║██╔████╔██║██╔████╔██║███████║██╔██╗ ██║██║  ██║ :
                : ██║     ██║   ██║██║╚██╔╝██║██║╚██╔╝██║██╔══██║██║╚██╗██║██║  ██║ :
                : ╚██████╗╚██████╔╝██║ ╚═╝ ██║██║ ╚═╝ ██║██║  ██║██║ ╚████║██████╔╝ :
                :  ╚═════╝ ╚═════╝ ╚═╝     ╚═╝╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═════╝  :
                :                     ███████╗██████╗ ██╗  ██╗                      :
                :                     ██╔════╝██╔══██╗██║ ██╔╝                      :
                :                     ███████╗██║  ██║█████╔╝                       :
                :                     ╚════██║██║  ██║██╔═██╗                       :
                :                     ███████║██████╔╝██║  ██╗                      :
                :                     ╚══════╝╚═════╝ ╚═╝  ╚═╝                      :
                ·····································································
                """;
        System.out.println(banner);
    }
}
