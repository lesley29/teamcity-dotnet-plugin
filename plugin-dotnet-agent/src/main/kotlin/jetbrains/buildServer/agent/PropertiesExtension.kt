package jetbrains.buildServer.agent

import jetbrains.buildServer.agent.config.AgentParametersSupplier
import jetbrains.buildServer.rx.use
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PropertiesExtension(
    private val _dispatcher: CoroutineDispatcher,
    private val _agentPropertiesProviders: List<AgentPropertiesProvider>
) : AgentParametersSupplier {
    private val _lockObject = Object()

    override fun getParameters(): MutableMap<String, String> {
        val parameters = mutableMapOf<String, String>()
        LOG.infoBlock("Fetched agent properties").use {
            return runBlocking {
                _agentPropertiesProviders.map { agentPropertiesProvider ->
                    launch(_dispatcher){
                        fetchProperties(agentPropertiesProvider, parameters)
                    }
                }.joinAll()
                parameters
            }
        }
    }

    private fun fetchProperties(agentPropertiesProvider: AgentPropertiesProvider, parameters: MutableMap<String, String>) {
        LOG.debugBlock("Fetching agent properties for ${agentPropertiesProvider.desription}").use {
            try {
                for (property in agentPropertiesProvider.properties) {
                    var name = property.name
                    synchronized(_lockObject) {
                        val prevValue = parameters.get(name)
                        if (prevValue != null) {
                            LOG.warn("Update ${name}=\"${property.value}\". Previous value was \"$prevValue\".")
                        } else {
                            LOG.info("${name}=\"${property.value}\".")
                        }

                        parameters.put(name, property.value)
                    }
                }
            } catch (e: Exception) {
                LOG.debug("Error while fetching the agent properties for ${agentPropertiesProvider.desription}", e)
            }
        }
    }

    companion object {
        private val LOG = Logger.getLogger(PropertiesExtension::class.java)
    }
}