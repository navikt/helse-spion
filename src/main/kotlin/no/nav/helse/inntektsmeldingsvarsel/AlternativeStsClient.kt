package no.nav.helse.inntektsmeldingsvarsel

import no.nav.syfo.ws.util.STSClientWSTrust13and14
import org.apache.cxf.Bus
import org.apache.cxf.binding.soap.Soap12
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.endpoint.Client
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.ws.policy.PolicyBuilder
import org.apache.cxf.ws.policy.PolicyEngine
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver
import org.apache.cxf.ws.security.trust.STSClient
import org.apache.neethi.Policy
import java.util.*

object STSClientConfig {
    // Only use no transportbinding on localhost, should use the requestSamlPolicy.xml with transport binding https
    // when in production.
    private const val STS_REQUEST_SAML_POLICY = "classpath:sts-policies/requestSamlPolicy.xml"
    private const val STS_CLIENT_AUTHENTICATION_POLICY = "classpath:sts-policies/untPolicy.xml"

    fun <T> configureRequestSamlToken(port: T, stsUrl: String, stsCredentials: Pair<String, String>): T {
        val client = ClientProxy.getClient(port)
        // do not have onbehalfof token so cache token in endpoint
        configureStsRequestSamlToken(client, true, stsUrl, stsCredentials)
        return port
    }

    private fun configureStsRequestSamlToken(client: Client, cacheTokenInEndpoint: Boolean, stsUrl: String, stsCredentials: Pair<String, String>) {
        // TODO: remove custom client when STS is updated to support the cxf client
        val stsClient = createCustomSTSClient(client.bus)
        configureStsWithPolicyForClient(stsClient, client, STS_REQUEST_SAML_POLICY, cacheTokenInEndpoint, stsUrl, stsCredentials)
    }

    private fun configureStsWithPolicyForClient(stsClient: STSClient,
                                                client: Client,
                                                policyReference: String,
                                                cacheTokenInEndpoint: Boolean,
                                                stsUrl: String,
                                                stsCredentials: Pair<String, String>) {

        configureSTSClient(stsClient, stsUrl, stsCredentials.first, stsCredentials.second)

        client.requestContext[org.apache.cxf.rt.security.SecurityConstants.STS_CLIENT] = stsClient
        client.requestContext[org.apache.cxf.rt.security.SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT] = cacheTokenInEndpoint
        setEndpointPolicyReference(client, policyReference)
    }

    /**
     * Creating custom STS client because the STS on Datapower requires KeyType as a child to RequestSecurityToken and
     * TokenType as a child to SecondaryParameters. Standard CXF client put both elements in SecondaryParameters. By
     * overriding the useSecondaryParameters method you can exactly specify the request in the
     * RequestSecurityTokenTemplate in the policy.
     *
     * @param bus
     * @return
     */
    private fun createCustomSTSClient(bus: Bus): STSClient {
        return STSClientWSTrust13and14(bus)
    }

    private fun configureSTSClient(stsClient: STSClient,
                                   location: String,
                                   username: String,
                                   password: String): STSClient {

        stsClient.isEnableAppliesTo = false
        stsClient.isAllowRenewing = false
        stsClient.location = location
        //stsClient.features = listOf(WSAddressingFeature(), LoggingFeature())

        val properties = HashMap<String, Any>()
        properties[org.apache.cxf.rt.security.SecurityConstants.USERNAME] = username
        properties[org.apache.cxf.rt.security.SecurityConstants.PASSWORD] = password

        stsClient.properties = properties

        // used for the STS client to authenticate itself to the STS provider.
        stsClient.setPolicy(STS_CLIENT_AUTHENTICATION_POLICY)
        return stsClient
    }

    private fun setEndpointPolicyReference(client: Client, uri: String) {
        val policy = resolvePolicyReference(client, uri)
        setClientEndpointPolicy(client, policy)
    }

    private fun resolvePolicyReference(client: Client, uri: String): Policy {
        val policyBuilder = client.bus.getExtension<PolicyBuilder>(PolicyBuilder::class.java)
        val resolver = RemoteReferenceResolver("", policyBuilder)
        return resolver.resolveReference(uri)
    }

    private fun setClientEndpointPolicy(client: Client, policy: Policy) {
        val endpoint = client.endpoint
        val endpointInfo = endpoint.endpointInfo

        val policyEngine = client.bus.getExtension<PolicyEngine>(PolicyEngine::class.java)
        val message = SoapMessage(Soap12.getInstance())
        val endpointPolicy = policyEngine.getClientEndpointPolicy(endpointInfo, null, message)
        policyEngine.setClientEndpointPolicy(endpointInfo, endpointPolicy.updatePolicy(policy, message))
    }
}