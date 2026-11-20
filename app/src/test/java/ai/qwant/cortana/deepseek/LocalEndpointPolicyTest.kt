package ai.qwant.cortana.deepseek

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalEndpointPolicyTest {
    @Test
    fun acceptsLoopbackAndPrivateIpv4() {
        assertTrue(LocalEndpointPolicy.isAllowedHost("localhost"))
        assertTrue(LocalEndpointPolicy.isAllowedHost("127.0.0.1"))
        assertTrue(LocalEndpointPolicy.isAllowedHost("10.0.0.5"))
        assertTrue(LocalEndpointPolicy.isAllowedHost("172.16.4.20"))
        assertTrue(LocalEndpointPolicy.isAllowedHost("192.168.1.50"))
    }

    @Test
    fun rejectsPublicAndNamedInternetHosts() {
        assertFalse(LocalEndpointPolicy.isAllowedHost("8.8.8.8"))
        assertFalse(LocalEndpointPolicy.isAllowedHost("example.com"))
        assertFalse(LocalEndpointPolicy.isAllowedHost("api.openai.com"))
    }
}
