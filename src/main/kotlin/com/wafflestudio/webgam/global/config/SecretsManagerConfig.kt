package com.wafflestudio.webgam.global.config

import com.google.gson.Gson
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

@Configuration
@Profile("dev | prod")
class SecretsManagerConfig : EnvironmentAware, BeanFactoryPostProcessor {

    private lateinit var environment: Environment

    override fun setEnvironment(environment: Environment) {
        this.environment = environment
    }

    companion object {
        private val client = SecretsManagerClient.builder().region(Region.AP_NORTHEAST_2).build()
    }

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        val secretName = environment.getProperty("secret-name")

        Gson().fromJson<Map<String, String>>(getSecret(secretName!!), Map::class.java).forEach {
                (key, value) -> System.setProperty(key, value)
        }
    }

    fun getSecret(secretName: String): String {
        val getSecretValueRequest = GetSecretValueRequest.builder().secretId(secretName).build()

        return client.getSecretValue(getSecretValueRequest).secretString()
    }
}