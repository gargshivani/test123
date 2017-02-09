package com.appoxee.android;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Shivani Garg on 2/8/2017.
 */
public class AppoxeeServicesTask extends DefaultTask {

    /**
     * The input is not technically optional but we want to control the error message.
     * Without @Optional, Gradle will complain itself the file is missing.
     */
    @InputFile
    @Optional
    public File quickstartFile;

    @OutputDirectory
    public File intermediateDir;


    @TaskAction
    public void action() throws IOException {
       System.out.println("action method calls");
       if (!quickstartFile.isFile()) {
            getLogger().warn("File " + quickstartFile.getName() + " is missing from module root folder." +
                    " The Appoxee Quickstart Plugin cannot function without it.");

            // Skip the rest of the actions because it would not make sense if `quickstartFile` is missing.
            return;
        }

        // delete content of outputdir.
      deleteFolder(intermediateDir);
         if (!intermediateDir.mkdirs()) {
            throw new GradleException("Failed to create folder: " + intermediateDir);
        }

        JsonElement root = new JsonParser().parse(Files.newReader(quickstartFile, Charsets.UTF_8));

        if (!root.isJsonObject()) {
            throw new GradleException("Malformed root json");
        }

        JsonObject rootObject = root.getAsJsonObject();

        Map<String, String> resValues = new TreeMap<String, String>();

        handleProjectNumber(rootObject, resValues);

        // write the values file.
        File values = new File(intermediateDir, "values");
        if (!values.exists() && !values.mkdirs()) {
            throw new GradleException("Failed to create folder: " + values);
        }

        Files.write(getValuesContent(resValues), new File(values, "values.xml"), Charsets.UTF_8);
    }

    /**
     * Handle project_info/project_number for @string/gcm_defaultSenderId, and fill the res map with the read value.
     * @param rootObject the root Json object.
     * @throws IOException
     */
    private void handleProjectNumber(JsonObject rootObject, Map<String, String> resValues)
            throws IOException {
       /* JsonObject projectInfo = rootObject.getAsJsonObject("project_info");
        if (projectInfo == null) {
            throw new GradleException("Missing project_info object");
        }*/

        JsonPrimitive googleId = rootObject.getAsJsonPrimitive("google_id");
        JsonPrimitive sdkKey = rootObject.getAsJsonPrimitive("sdk_Key");

        if (googleId == null || sdkKey == null) {
            throw new GradleException("Missing project_info");
        }

        resValues.put("google_id", googleId.getAsString());
        resValues.put("sdk_key", sdkKey.getAsString());
    }


    private static String getValuesContent(Map<String, String> entries) {
        StringBuilder sb = new StringBuilder(256);

        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n");

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            sb.append("    <string name=\"").append(entry.getKey()).append("\">")
                    .append(entry.getValue()).append("</string>\n");
        }

        sb.append("</resources>\n");

        return sb.toString();
    }

    private static void deleteFolder(final File folder) {
        System.out.println("delete folder calls");
        if (!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    if (!file.delete()) {
                        throw new GradleException("Failed to delete: " + file);
                    }
                }
            }
        }
        if (!folder.delete()) {
            throw new GradleException("Failed to delete: " + folder);
        }
    }
}




