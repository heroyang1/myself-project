package com.bachstage.course.job_stream_from_kafka_to_es.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * application.properties 配置读取器
 */
public class ApplicationPropertiesConfig
{
    private static final String CONFIG_FILE = "application.properties";
    private static final Properties PROPERTIES = loadProperties();

    public static String read(String propertyKey, String systemKey, String defaultValue)
    {
        String systemValue = System.getProperty(systemKey);
        if (systemValue != null && !systemValue.trim().isEmpty())
        {
            return systemValue.trim();
        }
        String envValue = System.getenv(systemKey);
        if (envValue != null && !envValue.trim().isEmpty())
        {
            return envValue.trim();
        }
        String propertyValue = PROPERTIES.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.trim().isEmpty())
        {
            return propertyValue.trim();
        }
        return defaultValue;
    }

    public static int readInt(String propertyKey, String systemKey, int defaultValue)
    {
        String value = read(propertyKey, systemKey, String.valueOf(defaultValue));
        return Integer.parseInt(value);
    }

    public static long readLong(String propertyKey, String systemKey, long defaultValue)
    {
        String value = read(propertyKey, systemKey, String.valueOf(defaultValue));
        return Long.parseLong(value);
    }

    private static Properties loadProperties()
    {
        Properties properties = new Properties();
        InputStream inputStream = ApplicationPropertiesConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        if (inputStream == null)
        {
            return properties;
        }
        try
        {
            properties.load(inputStream);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("读取配置文件失败: " + CONFIG_FILE, e);
        }
        finally
        {
            try
            {
                inputStream.close();
            }
            catch (IOException ignored)
            {
                // 配置已读取完毕，关闭失败不影响结果
            }
        }
        return properties;
    }
}
