package com.orctom.mojo.was.service.impl;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.orctom.mojo.was.model.WebSphereModel;
import com.orctom.mojo.was.model.WebSphereServiceException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.*;

/**
 * Created by CH on 3/13/14.
 */
public class CommandUtils {

    public static File getExecutable(final String wasHome, String name) {
        if (StringUtils.isBlank(wasHome)) {
            throw new WebSphereServiceException("WAS_HOME is not set");
        }
        File binDir = new File(wasHome, "bin");
        File[] candidates = binDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                dir.equals(wasHome);
                return name.startsWith(name);
            }
        });

        if (candidates.length != 1) {
            throw new WebSphereServiceException("Couldn't find " + name + "[.sh|.bat], candidates: " + candidates);
        }

        File executable = candidates[0];
        System.out.println(name + " location: " + executable.getAbsolutePath());

        return executable;
    }

    public static File getBuildScript(String task, String template, WebSphereModel model, String workingDir, String ext)
            throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(template);

        StringBuilder buildFile = new StringBuilder(50);
        buildFile.append(task);
        if (StringUtils.isNotBlank(model.getHost())) {
            buildFile.append("-").append(model.getHost());
        }
        if (StringUtils.isNotBlank(model.getApplicationName())) {
            buildFile.append("-").append(model.getApplicationName());
        }
        buildFile.append("-").append(System.currentTimeMillis()).append(ext);

        File buildScriptFile = new File(workingDir, buildFile.toString());
        buildScriptFile.getParentFile().mkdirs();
        Writer writer = new FileWriter(buildScriptFile);
        mustache.execute(writer, model).flush();

        return buildScriptFile;
    }

    public static void executeCommand(Commandline commandline, StreamConsumer outConsumer, StreamConsumer errorConsumer,
                                      boolean isVerbose) {
        try {
            if (isVerbose) {
                System.out.println("Executing command: " + StringUtils.join(commandline.getShellCommandline(), " "));
            }

            int returnCode = CommandLineUtils.executeCommandLine(commandline, outConsumer, errorConsumer, 300);

            String msg = "Return code: " + returnCode;
            if (returnCode != 0) {
                throw new WebSphereServiceException(msg);
            } else {
                System.out.println(msg);
            }

        } catch (CommandLineException e) {
            throw new WebSphereServiceException(e.getMessage());
        }
    }
}
