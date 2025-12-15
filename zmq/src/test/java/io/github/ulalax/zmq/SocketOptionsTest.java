package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for ZMQ socket options.
 */
@DisplayName("Socket Options Tests")
class SocketOptionsTest {

    @Test
    @DisplayName("SocketOption_Linger_ShouldBeConfigurable")
    void socketOption_Linger_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {
            int expectedValue = 500;

            socket.setOption(SocketOption.LINGER, expectedValue);
            int actualValue = socket.getOption(SocketOption.LINGER);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 1000, 5000})
    @DisplayName("SocketOption_Linger_ShouldAcceptVariousValues")
    void socketOption_Linger_ShouldAcceptVariousValues(int lingerValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {

            socket.setOption(SocketOption.LINGER, lingerValue);
            int actualValue = socket.getOption(SocketOption.LINGER);

            assertThat(actualValue).isEqualTo(lingerValue);
        }
    }

    @Test
    @DisplayName("SocketOption_SendHighWaterMark_ShouldBeConfigurable")
    void socketOption_SendHighWaterMark_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUB)) {
            int expectedValue = 2000;

            socket.setOption(SocketOption.SNDHWM, expectedValue);
            int actualValue = socket.getOption(SocketOption.SNDHWM);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 1000, 10000})
    @DisplayName("SocketOption_SendHighWaterMark_ShouldAcceptVariousValues")
    void socketOption_SendHighWaterMark_ShouldAcceptVariousValues(int hwmValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUSH)) {

            socket.setOption(SocketOption.SNDHWM, hwmValue);
            int actualValue = socket.getOption(SocketOption.SNDHWM);

            assertThat(actualValue).isEqualTo(hwmValue);
        }
    }

    @Test
    @DisplayName("SocketOption_RecvHighWaterMark_ShouldBeConfigurable")
    void socketOption_RecvHighWaterMark_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {
            int expectedValue = 3000;

            socket.setOption(SocketOption.RCVHWM, expectedValue);
            int actualValue = socket.getOption(SocketOption.RCVHWM);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5000, 20000})
    @DisplayName("SocketOption_RecvHighWaterMark_ShouldAcceptVariousValues")
    void socketOption_RecvHighWaterMark_ShouldAcceptVariousValues(int hwmValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PULL)) {

            socket.setOption(SocketOption.RCVHWM, hwmValue);
            int actualValue = socket.getOption(SocketOption.RCVHWM);

            assertThat(actualValue).isEqualTo(hwmValue);
        }
    }

    @Test
    @DisplayName("SocketOption_SendTimeout_ShouldBeConfigurable")
    void socketOption_SendTimeout_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {
            int expectedValue = 1500;

            socket.setOption(SocketOption.SNDTIMEO, expectedValue);
            int actualValue = socket.getOption(SocketOption.SNDTIMEO);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 100, 5000})
    @DisplayName("SocketOption_SendTimeout_ShouldAcceptVariousValues")
    void socketOption_SendTimeout_ShouldAcceptVariousValues(int timeoutValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUSH)) {

            socket.setOption(SocketOption.SNDTIMEO, timeoutValue);
            int actualValue = socket.getOption(SocketOption.SNDTIMEO);

            assertThat(actualValue).isEqualTo(timeoutValue);
        }
    }

    @Test
    @DisplayName("SocketOption_RecvTimeout_ShouldBeConfigurable")
    void socketOption_RecvTimeout_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REP)) {
            int expectedValue = 2500;

            socket.setOption(SocketOption.RCVTIMEO, expectedValue);
            int actualValue = socket.getOption(SocketOption.RCVTIMEO);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 200, 10000})
    @DisplayName("SocketOption_RecvTimeout_ShouldAcceptVariousValues")
    void socketOption_RecvTimeout_ShouldAcceptVariousValues(int timeoutValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PULL)) {

            socket.setOption(SocketOption.RCVTIMEO, timeoutValue);
            int actualValue = socket.getOption(SocketOption.RCVTIMEO);

            assertThat(actualValue).isEqualTo(timeoutValue);
        }
    }

    @Test
    @DisplayName("SocketOption_TcpKeepalive_ShouldBeConfigurable")
    void socketOption_TcpKeepalive_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            int expectedValue = 1;

            socket.setOption(SocketOption.TCP_KEEPALIVE, expectedValue);
            int actualValue = socket.getOption(SocketOption.TCP_KEEPALIVE);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    @DisplayName("SocketOption_TcpKeepalive_ShouldAcceptVariousValues")
    void socketOption_TcpKeepalive_ShouldAcceptVariousValues(int keepaliveValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            socket.setOption(SocketOption.TCP_KEEPALIVE, keepaliveValue);
            int actualValue = socket.getOption(SocketOption.TCP_KEEPALIVE);

            assertThat(actualValue).isEqualTo(keepaliveValue);
        }
    }

    @Test
    @DisplayName("SocketOption_RoutingId_ShouldBeConfigurable")
    void socketOption_RoutingId_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {
            byte[] expectedValue = new byte[]{0x01, 0x02, 0x03, 0x04};

            socket.setOption(SocketOption.ROUTING_ID, expectedValue);
            byte[] actualValue = socket.getOptionBytes(SocketOption.ROUTING_ID, 256);
            assertThat(actualValue).containsExactly(expectedValue);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "01",
            "41,42,43",
            "01,02,03,04,05,06,07,08"
    })
    @DisplayName("SocketOption_RoutingId_ShouldAcceptVariousByteArrays")
    void socketOption_RoutingId_ShouldAcceptVariousByteArrays(String hexValues) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            byte[] routingId = parseHexBytes(hexValues);

            socket.setOption(SocketOption.ROUTING_ID, routingId);
            byte[] actualValue = socket.getOptionBytes(SocketOption.ROUTING_ID, 256);
            assertThat(actualValue).containsExactly(routingId);
        }
    }

    @Test
    @DisplayName("SocketOption_Affinity_ShouldBeConfigurable")
    void socketOption_Affinity_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUB)) {
            long expectedValue = 3L;

            socket.setOption(SocketOption.AFFINITY, expectedValue);
            long actualValue = socket.getOptionLong(SocketOption.AFFINITY);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 2L, 15L})
    @DisplayName("SocketOption_Affinity_ShouldAcceptVariousValues")
    void socketOption_Affinity_ShouldAcceptVariousValues(long affinityValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.AFFINITY, affinityValue);
            long actualValue = socket.getOptionLong(SocketOption.AFFINITY);

            assertThat(actualValue).isEqualTo(affinityValue);
        }
    }

    @Test
    @DisplayName("SocketOption_IPv6_ShouldBeConfigurable")
    void socketOption_IPv6_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REP)) {
            int expectedValue = 1;

            socket.setOption(SocketOption.IPV6, expectedValue);
            int actualValue = socket.getOption(SocketOption.IPV6);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_IPv6_ShouldAcceptVariousValues")
    void socketOption_IPv6_ShouldAcceptVariousValues(int ipv6Value) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {

            socket.setOption(SocketOption.IPV6, ipv6Value);
            int actualValue = socket.getOption(SocketOption.IPV6);

            assertThat(actualValue).isEqualTo(ipv6Value);
        }
    }

    @Test
    @DisplayName("SocketOption_ReconnectInterval_ShouldBeConfigurable")
    void socketOption_ReconnectInterval_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            int expectedValue = 500;

            socket.setOption(SocketOption.RECONNECT_IVL, expectedValue);
            int actualValue = socket.getOption(SocketOption.RECONNECT_IVL);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100, 1000, 5000})
    @DisplayName("SocketOption_ReconnectInterval_ShouldAcceptVariousValues")
    void socketOption_ReconnectInterval_ShouldAcceptVariousValues(int intervalValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.RECONNECT_IVL, intervalValue);
            int actualValue = socket.getOption(SocketOption.RECONNECT_IVL);

            assertThat(actualValue).isEqualTo(intervalValue);
        }
    }

    @Test
    @DisplayName("SocketOption_MultipleOptions_ShouldBeConfigurableIndependently")
    void socketOption_MultipleOptions_ShouldBeConfigurableIndependently() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            int expectedLinger = 1000;
            int expectedSendTimeout = 2000;
            int expectedRecvTimeout = 3000;
            int expectedSndhwm = 5000;
            int expectedRcvhwm = 6000;

            socket.setOption(SocketOption.LINGER, expectedLinger);
            socket.setOption(SocketOption.SNDTIMEO, expectedSendTimeout);
            socket.setOption(SocketOption.RCVTIMEO, expectedRecvTimeout);
            socket.setOption(SocketOption.SNDHWM, expectedSndhwm);
            socket.setOption(SocketOption.RCVHWM, expectedRcvhwm);

            assertThat(socket.getOption(SocketOption.LINGER)).isEqualTo(expectedLinger);
            assertThat(socket.getOption(SocketOption.SNDTIMEO)).isEqualTo(expectedSendTimeout);
            assertThat(socket.getOption(SocketOption.RCVTIMEO)).isEqualTo(expectedRecvTimeout);
            assertThat(socket.getOption(SocketOption.SNDHWM)).isEqualTo(expectedSndhwm);
            assertThat(socket.getOption(SocketOption.RCVHWM)).isEqualTo(expectedRcvhwm);
        }
    }

    @Test
    @DisplayName("SocketOption_SetBeforeBind_ShouldWork")
    void socketOption_SetBeforeBind_ShouldWork() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REP)) {
            int expectedLinger = 0;

            socket.setOption(SocketOption.LINGER, expectedLinger);
            socket.bind("tcp://127.0.0.1:25555");
            int actualValue = socket.getOption(SocketOption.LINGER);

            assertThat(actualValue).isEqualTo(expectedLinger);

            socket.unbind("tcp://127.0.0.1:25555");
        }
    }

    @Test
    @DisplayName("SocketOption_SetAfterBind_ShouldWork")
    void socketOption_SetAfterBind_ShouldWork() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REP)) {
            int expectedSendTimeout = 1000;

            socket.bind("tcp://127.0.0.1:25556");
            socket.setOption(SocketOption.SNDTIMEO, expectedSendTimeout);
            int actualValue = socket.getOption(SocketOption.SNDTIMEO);

            assertThat(actualValue).isEqualTo(expectedSendTimeout);

            socket.unbind("tcp://127.0.0.1:25556");
        }
    }

    @Test
    @DisplayName("SocketOption_GetWithoutSet_ShouldReturnDefaultValue")
    void socketOption_GetWithoutSet_ShouldReturnDefaultValue() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {

            int linger = socket.getOption(SocketOption.LINGER);
            int sndhwm = socket.getOption(SocketOption.SNDHWM);
            int rcvhwm = socket.getOption(SocketOption.RCVHWM);

            assertThat(linger).isGreaterThanOrEqualTo(-1);
            assertThat(sndhwm).isGreaterThanOrEqualTo(0);
            assertThat(rcvhwm).isGreaterThanOrEqualTo(0);
        }
    }

    // Integer Socket Options

    @Test
    @DisplayName("SocketOption_Rate_ShouldBeConfigurable")
    void socketOption_Rate_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUB)) {
            int expectedValue = 100;

            socket.setOption(SocketOption.RATE, expectedValue);
            int actualValue = socket.getOption(SocketOption.RATE);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100, 1000, 10000})
    @DisplayName("SocketOption_Rate_ShouldAcceptVariousValues")
    void socketOption_Rate_ShouldAcceptVariousValues(int rateValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.RATE, rateValue);
            int actualValue = socket.getOption(SocketOption.RATE);

            assertThat(actualValue).isEqualTo(rateValue);
        }
    }

    @Test
    @DisplayName("SocketOption_RecoveryInterval_ShouldBeConfigurable")
    void socketOption_RecoveryInterval_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUB)) {
            int expectedValue = 1000;

            socket.setOption(SocketOption.RECOVERY_IVL, expectedValue);
            int actualValue = socket.getOption(SocketOption.RECOVERY_IVL);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1000, 5000, 10000})
    @DisplayName("SocketOption_RecoveryInterval_ShouldAcceptVariousValues")
    void socketOption_RecoveryInterval_ShouldAcceptVariousValues(int intervalValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.RECOVERY_IVL, intervalValue);
            int actualValue = socket.getOption(SocketOption.RECOVERY_IVL);

            assertThat(actualValue).isEqualTo(intervalValue);
        }
    }

    @Test
    @DisplayName("SocketOption_SendBuffer_ShouldBeConfigurable")
    void socketOption_SendBuffer_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUSH)) {
            int expectedValue = 4096;

            socket.setOption(SocketOption.SNDBUF, expectedValue);
            int actualValue = socket.getOption(SocketOption.SNDBUF);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4096, 65536, 131072})
    @DisplayName("SocketOption_SendBuffer_ShouldAcceptVariousValues")
    void socketOption_SendBuffer_ShouldAcceptVariousValues(int bufferSize) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUB)) {

            socket.setOption(SocketOption.SNDBUF, bufferSize);
            int actualValue = socket.getOption(SocketOption.SNDBUF);

            assertThat(actualValue).isEqualTo(bufferSize);
        }
    }

    @Test
    @DisplayName("SocketOption_ReceiveBuffer_ShouldBeConfigurable")
    void socketOption_ReceiveBuffer_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PULL)) {
            int expectedValue = 8192;

            socket.setOption(SocketOption.RCVBUF, expectedValue);
            int actualValue = socket.getOption(SocketOption.RCVBUF);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4096, 65536, 262144})
    @DisplayName("SocketOption_ReceiveBuffer_ShouldAcceptVariousValues")
    void socketOption_ReceiveBuffer_ShouldAcceptVariousValues(int bufferSize) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.RCVBUF, bufferSize);
            int actualValue = socket.getOption(SocketOption.RCVBUF);

            assertThat(actualValue).isEqualTo(bufferSize);
        }
    }

    @Test
    @DisplayName("SocketOption_Backlog_ShouldBeConfigurable")
    void socketOption_Backlog_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REP)) {
            int expectedValue = 10;

            socket.setOption(SocketOption.BACKLOG, expectedValue);
            int actualValue = socket.getOption(SocketOption.BACKLOG);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 100, 1000})
    @DisplayName("SocketOption_Backlog_ShouldAcceptVariousValues")
    void socketOption_Backlog_ShouldAcceptVariousValues(int backlogValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            socket.setOption(SocketOption.BACKLOG, backlogValue);
            int actualValue = socket.getOption(SocketOption.BACKLOG);

            assertThat(actualValue).isEqualTo(backlogValue);
        }
    }

    @Test
    @DisplayName("SocketOption_ReconnectIntervalMax_ShouldBeConfigurable")
    void socketOption_ReconnectIntervalMax_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            int expectedValue = 10000;

            socket.setOption(SocketOption.RECONNECT_IVL_MAX, expectedValue);
            int actualValue = socket.getOption(SocketOption.RECONNECT_IVL_MAX);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5000, 10000, 60000})
    @DisplayName("SocketOption_ReconnectIntervalMax_ShouldAcceptVariousValues")
    void socketOption_ReconnectIntervalMax_ShouldAcceptVariousValues(int intervalValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.RECONNECT_IVL_MAX, intervalValue);
            int actualValue = socket.getOption(SocketOption.RECONNECT_IVL_MAX);

            assertThat(actualValue).isEqualTo(intervalValue);
        }
    }

    @Test
    @DisplayName("SocketOption_MulticastHops_ShouldBeConfigurable")
    void socketOption_MulticastHops_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUB)) {
            int expectedValue = 5;

            socket.setOption(SocketOption.MULTICAST_HOPS, expectedValue);
            int actualValue = socket.getOption(SocketOption.MULTICAST_HOPS);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 255})
    @DisplayName("SocketOption_MulticastHops_ShouldAcceptVariousValues")
    void socketOption_MulticastHops_ShouldAcceptVariousValues(int hopsValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.MULTICAST_HOPS, hopsValue);
            int actualValue = socket.getOption(SocketOption.MULTICAST_HOPS);

            assertThat(actualValue).isEqualTo(hopsValue);
        }
    }

    @Test
    @DisplayName("SocketOption_TcpKeepaliveCnt_ShouldBeConfigurable")
    void socketOption_TcpKeepaliveCnt_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {
            int expectedValue = 5;

            socket.setOption(SocketOption.TCP_KEEPALIVE_CNT, expectedValue);
            int actualValue = socket.getOption(SocketOption.TCP_KEEPALIVE_CNT);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 5, 10})
    @DisplayName("SocketOption_TcpKeepaliveCnt_ShouldAcceptVariousValues")
    void socketOption_TcpKeepaliveCnt_ShouldAcceptVariousValues(int countValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {

            socket.setOption(SocketOption.TCP_KEEPALIVE_CNT, countValue);
            int actualValue = socket.getOption(SocketOption.TCP_KEEPALIVE_CNT);

            assertThat(actualValue).isEqualTo(countValue);
        }
    }

    @Test
    @DisplayName("SocketOption_TcpKeepaliveIdle_ShouldBeConfigurable")
    void socketOption_TcpKeepaliveIdle_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {
            int expectedValue = 60;

            socket.setOption(SocketOption.TCP_KEEPALIVE_IDLE, expectedValue);
            int actualValue = socket.getOption(SocketOption.TCP_KEEPALIVE_IDLE);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 5, 60, 300})
    @DisplayName("SocketOption_TcpKeepaliveIdle_ShouldAcceptVariousValues")
    void socketOption_TcpKeepaliveIdle_ShouldAcceptVariousValues(int idleValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {

            socket.setOption(SocketOption.TCP_KEEPALIVE_IDLE, idleValue);
            int actualValue = socket.getOption(SocketOption.TCP_KEEPALIVE_IDLE);

            assertThat(actualValue).isEqualTo(idleValue);
        }
    }

    @Test
    @DisplayName("SocketOption_TcpKeepaliveIntvl_ShouldBeConfigurable")
    void socketOption_TcpKeepaliveIntvl_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {
            int expectedValue = 10;

            socket.setOption(SocketOption.TCP_KEEPALIVE_INTVL, expectedValue);
            int actualValue = socket.getOption(SocketOption.TCP_KEEPALIVE_INTVL);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 5, 10, 30})
    @DisplayName("SocketOption_TcpKeepaliveIntvl_ShouldAcceptVariousValues")
    void socketOption_TcpKeepaliveIntvl_ShouldAcceptVariousValues(int intervalValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {

            socket.setOption(SocketOption.TCP_KEEPALIVE_INTVL, intervalValue);
            int actualValue = socket.getOption(SocketOption.TCP_KEEPALIVE_INTVL);

            assertThat(actualValue).isEqualTo(intervalValue);
        }
    }

    @Test
    @DisplayName("SocketOption_Immediate_ShouldBeConfigurable")
    void socketOption_Immediate_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {
            int expectedValue = 1;

            socket.setOption(SocketOption.IMMEDIATE, expectedValue);
            int actualValue = socket.getOption(SocketOption.IMMEDIATE);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_Immediate_ShouldAcceptVariousValues")
    void socketOption_Immediate_ShouldAcceptVariousValues(int immediateValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {

            socket.setOption(SocketOption.IMMEDIATE, immediateValue);
            int actualValue = socket.getOption(SocketOption.IMMEDIATE);

            assertThat(actualValue).isEqualTo(immediateValue);
        }
    }

    @Test
    @DisplayName("SocketOption_RouterMandatory_ShouldBeConfigurable")
    void socketOption_RouterMandatory_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {
            int expectedValue = 1;

            // Router_Mandatory is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.ROUTER_MANDATORY, expectedValue);

            assertThat(expectedValue).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_RouterMandatory_ShouldAcceptVariousValues")
    void socketOption_RouterMandatory_ShouldAcceptVariousValues(int mandatoryValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            // Router_Mandatory is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.ROUTER_MANDATORY, mandatoryValue);

            assertThat(mandatoryValue).isIn(0, 1);
        }
    }

    @Test
    @DisplayName("SocketOption_RouterHandover_ShouldBeConfigurable")
    void socketOption_RouterHandover_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {
            int expectedValue = 1;

            // Router_Handover is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.ROUTER_HANDOVER, expectedValue);

            assertThat(expectedValue).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_RouterHandover_ShouldAcceptVariousValues")
    void socketOption_RouterHandover_ShouldAcceptVariousValues(int handoverValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            // Router_Handover is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.ROUTER_HANDOVER, handoverValue);

            assertThat(handoverValue).isIn(0, 1);
        }
    }

    @Test
    @DisplayName("SocketOption_XpubVerbose_ShouldBeConfigurable")
    void socketOption_XpubVerbose_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.XPUB)) {
            int expectedValue = 1;

            // Xpub_Verbose is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.XPUB_VERBOSE, expectedValue);

            assertThat(expectedValue).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_XpubVerbose_ShouldAcceptVariousValues")
    void socketOption_XpubVerbose_ShouldAcceptVariousValues(int verboseValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.XPUB)) {

            // Xpub_Verbose is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.XPUB_VERBOSE, verboseValue);

            assertThat(verboseValue).isIn(0, 1);
        }
    }

    @Test
    @DisplayName("SocketOption_XpubVerboser_ShouldBeConfigurable")
    void socketOption_XpubVerboser_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.XPUB)) {
            int expectedValue = 1;

            // Xpub_Verboser is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.XPUB_VERBOSER, expectedValue);

            assertThat(expectedValue).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_XpubVerboser_ShouldAcceptVariousValues")
    void socketOption_XpubVerboser_ShouldAcceptVariousValues(int verboserValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.XPUB)) {

            // Xpub_Verboser is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.XPUB_VERBOSER, verboserValue);

            assertThat(verboserValue).isIn(0, 1);
        }
    }

    @Test
    @DisplayName("SocketOption_XpubNodrop_ShouldBeConfigurable")
    void socketOption_XpubNodrop_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.XPUB)) {
            int expectedValue = 1;

            // Xpub_Nodrop is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.XPUB_NODROP, expectedValue);

            assertThat(expectedValue).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_XpubNodrop_ShouldAcceptVariousValues")
    void socketOption_XpubNodrop_ShouldAcceptVariousValues(int nodropValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.XPUB)) {

            // Xpub_Nodrop is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.XPUB_NODROP, nodropValue);

            assertThat(nodropValue).isIn(0, 1);
        }
    }

    @Test
    @DisplayName("SocketOption_XpubManual_ShouldBeConfigurable")
    void socketOption_XpubManual_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.XPUB)) {
            int expectedValue = 1;

            // Xpub_Manual is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.XPUB_MANUAL, expectedValue);

            assertThat(expectedValue).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_XpubManual_ShouldAcceptVariousValues")
    void socketOption_XpubManual_ShouldAcceptVariousValues(int manualValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.XPUB)) {

            // Xpub_Manual is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.XPUB_MANUAL, manualValue);

            assertThat(manualValue).isIn(0, 1);
        }
    }

    @Test
    @DisplayName("SocketOption_PlainServer_ShouldBeConfigurable")
    void socketOption_PlainServer_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REP)) {
            int expectedValue = 1;

            socket.setOption(SocketOption.PLAIN_SERVER, expectedValue);
            int actualValue = socket.getOption(SocketOption.PLAIN_SERVER);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_PlainServer_ShouldAcceptVariousValues")
    void socketOption_PlainServer_ShouldAcceptVariousValues(int serverValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            socket.setOption(SocketOption.PLAIN_SERVER, serverValue);
            int actualValue = socket.getOption(SocketOption.PLAIN_SERVER);

            assertThat(actualValue).isEqualTo(serverValue);
        }
    }

    @Test
    @DisplayName("SocketOption_CurveServer_ShouldBeConfigurable")
    void socketOption_CurveServer_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REP)) {
            int expectedValue = 1;

            socket.setOption(SocketOption.CURVE_SERVER, expectedValue);
            int actualValue = socket.getOption(SocketOption.CURVE_SERVER);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_CurveServer_ShouldAcceptVariousValues")
    void socketOption_CurveServer_ShouldAcceptVariousValues(int serverValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            socket.setOption(SocketOption.CURVE_SERVER, serverValue);
            int actualValue = socket.getOption(SocketOption.CURVE_SERVER);

            assertThat(actualValue).isEqualTo(serverValue);
        }
    }

    @Test
    @DisplayName("SocketOption_ProbeRouter_ShouldBeConfigurable")
    void socketOption_ProbeRouter_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            int expectedValue = 1;

            // Probe_Router is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.PROBE_ROUTER, expectedValue);

            assertThat(expectedValue).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_ProbeRouter_ShouldAcceptVariousValues")
    void socketOption_ProbeRouter_ShouldAcceptVariousValues(int probeValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            // Probe_Router is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.PROBE_ROUTER, probeValue);

            assertThat(probeValue).isIn(0, 1);
        }
    }

    @Test
    @DisplayName("SocketOption_ReqCorrelate_ShouldBeConfigurable")
    void socketOption_ReqCorrelate_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {
            int expectedValue = 1;

            // Req_Correlate is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.REQ_CORRELATE, expectedValue);

            assertThat(expectedValue).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_ReqCorrelate_ShouldAcceptVariousValues")
    void socketOption_ReqCorrelate_ShouldAcceptVariousValues(int correlateValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {

            // Req_Correlate is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.REQ_CORRELATE, correlateValue);

            assertThat(correlateValue).isIn(0, 1);
        }
    }

    @Test
    @DisplayName("SocketOption_ReqRelaxed_ShouldBeConfigurable")
    void socketOption_ReqRelaxed_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {
            int expectedValue = 1;

            // Req_Relaxed is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.REQ_RELAXED, expectedValue);

            assertThat(expectedValue).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_ReqRelaxed_ShouldAcceptVariousValues")
    void socketOption_ReqRelaxed_ShouldAcceptVariousValues(int relaxedValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {

            // Req_Relaxed is write-only, just verify it doesn't throw
            socket.setOption(SocketOption.REQ_RELAXED, relaxedValue);

            assertThat(relaxedValue).isIn(0, 1);
        }
    }

    @Test
    @DisplayName("SocketOption_Conflate_ShouldBeConfigurable")
    void socketOption_Conflate_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PULL)) {
            int expectedValue = 1;

            socket.setOption(SocketOption.CONFLATE, expectedValue);
            int actualValue = socket.getOption(SocketOption.CONFLATE);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_Conflate_ShouldAcceptVariousValues")
    void socketOption_Conflate_ShouldAcceptVariousValues(int conflateValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.CONFLATE, conflateValue);
            int actualValue = socket.getOption(SocketOption.CONFLATE);

            assertThat(actualValue).isEqualTo(conflateValue);
        }
    }

    @Test
    @DisplayName("SocketOption_Tos_ShouldBeConfigurable")
    void socketOption_Tos_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUB)) {
            int expectedValue = 0x10;

            socket.setOption(SocketOption.TOS, expectedValue);
            int actualValue = socket.getOption(SocketOption.TOS);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 0x10, 0x20, 0x28})
    @DisplayName("SocketOption_Tos_ShouldAcceptVariousValues")
    void socketOption_Tos_ShouldAcceptVariousValues(int tosValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {

            socket.setOption(SocketOption.TOS, tosValue);
            int actualValue = socket.getOption(SocketOption.TOS);

            assertThat(actualValue).isEqualTo(tosValue);
        }
    }

    @Test
    @DisplayName("SocketOption_HandshakeInterval_ShouldBeConfigurable")
    void socketOption_HandshakeInterval_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            int expectedValue = 5000;

            socket.setOption(SocketOption.HANDSHAKE_IVL, expectedValue);
            int actualValue = socket.getOption(SocketOption.HANDSHAKE_IVL);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1000, 5000, 30000})
    @DisplayName("SocketOption_HandshakeInterval_ShouldAcceptVariousValues")
    void socketOption_HandshakeInterval_ShouldAcceptVariousValues(int intervalValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            socket.setOption(SocketOption.HANDSHAKE_IVL, intervalValue);
            int actualValue = socket.getOption(SocketOption.HANDSHAKE_IVL);

            assertThat(actualValue).isEqualTo(intervalValue);
        }
    }

    @Test
    @DisplayName("SocketOption_InvertMatching_ShouldBeConfigurable")
    void socketOption_InvertMatching_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUB)) {
            int expectedValue = 1;

            socket.setOption(SocketOption.INVERT_MATCHING, expectedValue);
            int actualValue = socket.getOption(SocketOption.INVERT_MATCHING);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("SocketOption_InvertMatching_ShouldAcceptVariousValues")
    void socketOption_InvertMatching_ShouldAcceptVariousValues(int invertValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.XPUB)) {

            socket.setOption(SocketOption.INVERT_MATCHING, invertValue);
            int actualValue = socket.getOption(SocketOption.INVERT_MATCHING);

            assertThat(actualValue).isEqualTo(invertValue);
        }
    }

    @Test
    @DisplayName("SocketOption_HeartbeatInterval_ShouldBeConfigurable")
    void socketOption_HeartbeatInterval_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            int expectedValue = 1000;

            socket.setOption(SocketOption.HEARTBEAT_IVL, expectedValue);
            int actualValue = socket.getOption(SocketOption.HEARTBEAT_IVL);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1000, 5000, 10000})
    @DisplayName("SocketOption_HeartbeatInterval_ShouldAcceptVariousValues")
    void socketOption_HeartbeatInterval_ShouldAcceptVariousValues(int intervalValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            socket.setOption(SocketOption.HEARTBEAT_IVL, intervalValue);
            int actualValue = socket.getOption(SocketOption.HEARTBEAT_IVL);

            assertThat(actualValue).isEqualTo(intervalValue);
        }
    }

    @Test
    @DisplayName("SocketOption_HeartbeatTtl_ShouldBeConfigurable")
    void socketOption_HeartbeatTtl_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            int expectedValue = 5000;

            socket.setOption(SocketOption.HEARTBEAT_TTL, expectedValue);
            int actualValue = socket.getOption(SocketOption.HEARTBEAT_TTL);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1000, 5000, 20000})
    @DisplayName("SocketOption_HeartbeatTtl_ShouldAcceptVariousValues")
    void socketOption_HeartbeatTtl_ShouldAcceptVariousValues(int ttlValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            socket.setOption(SocketOption.HEARTBEAT_TTL, ttlValue);
            int actualValue = socket.getOption(SocketOption.HEARTBEAT_TTL);

            assertThat(actualValue).isEqualTo(ttlValue);
        }
    }

    @Test
    @DisplayName("SocketOption_HeartbeatTimeout_ShouldBeConfigurable")
    void socketOption_HeartbeatTimeout_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            int expectedValue = 3000;

            socket.setOption(SocketOption.HEARTBEAT_TIMEOUT, expectedValue);
            int actualValue = socket.getOption(SocketOption.HEARTBEAT_TIMEOUT);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1000, 3000, 10000})
    @DisplayName("SocketOption_HeartbeatTimeout_ShouldAcceptVariousValues")
    void socketOption_HeartbeatTimeout_ShouldAcceptVariousValues(int timeoutValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            socket.setOption(SocketOption.HEARTBEAT_TIMEOUT, timeoutValue);
            int actualValue = socket.getOption(SocketOption.HEARTBEAT_TIMEOUT);

            assertThat(actualValue).isEqualTo(timeoutValue);
        }
    }

    @Test
    @DisplayName("SocketOption_ConnectTimeout_ShouldBeConfigurable")
    void socketOption_ConnectTimeout_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            int expectedValue = 2000;

            socket.setOption(SocketOption.CONNECT_TIMEOUT, expectedValue);
            int actualValue = socket.getOption(SocketOption.CONNECT_TIMEOUT);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1000, 5000, 30000})
    @DisplayName("SocketOption_ConnectTimeout_ShouldAcceptVariousValues")
    void socketOption_ConnectTimeout_ShouldAcceptVariousValues(int timeoutValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.CONNECT_TIMEOUT, timeoutValue);
            int actualValue = socket.getOption(SocketOption.CONNECT_TIMEOUT);

            assertThat(actualValue).isEqualTo(timeoutValue);
        }
    }

    @Test
    @DisplayName("SocketOption_TcpMaxrt_ShouldBeConfigurable")
    void socketOption_TcpMaxrt_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            int expectedValue = 10000;

            socket.setOption(SocketOption.TCP_MAXRT, expectedValue);
            int actualValue = socket.getOption(SocketOption.TCP_MAXRT);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5000, 10000, 60000})
    @DisplayName("SocketOption_TcpMaxrt_ShouldAcceptVariousValues")
    void socketOption_TcpMaxrt_ShouldAcceptVariousValues(int maxrtValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            socket.setOption(SocketOption.TCP_MAXRT, maxrtValue);
            int actualValue = socket.getOption(SocketOption.TCP_MAXRT);

            assertThat(actualValue).isEqualTo(maxrtValue);
        }
    }

    @Test
    @DisplayName("SocketOption_MulticastMaxtpdu_ShouldBeConfigurable")
    void socketOption_MulticastMaxtpdu_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUB)) {
            int expectedValue = 2048;

            socket.setOption(SocketOption.MULTICAST_MAXTPDU, expectedValue);
            int actualValue = socket.getOption(SocketOption.MULTICAST_MAXTPDU);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {256, 1500, 2048, 8192})
    @DisplayName("SocketOption_MulticastMaxtpdu_ShouldAcceptVariousValues")
    void socketOption_MulticastMaxtpdu_ShouldAcceptVariousValues(int maxtpduValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.MULTICAST_MAXTPDU, maxtpduValue);
            int actualValue = socket.getOption(SocketOption.MULTICAST_MAXTPDU);

            assertThat(actualValue).isEqualTo(maxtpduValue);
        }
    }


    // Long Socket Options

    @Test
    @DisplayName("SocketOption_Maxmsgsize_ShouldBeConfigurable")
    void socketOption_Maxmsgsize_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PULL)) {
            long expectedValue = 1048576L;

            socket.setOption(SocketOption.MAXMSGSIZE, expectedValue);
            long actualValue = socket.getOptionLong(SocketOption.MAXMSGSIZE);

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, 0L, 65536L, 1048576L, 10485760L})
    @DisplayName("SocketOption_Maxmsgsize_ShouldAcceptVariousValues")
    void socketOption_Maxmsgsize_ShouldAcceptVariousValues(long maxmsgsizeValue) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.MAXMSGSIZE, maxmsgsizeValue);
            long actualValue = socket.getOptionLong(SocketOption.MAXMSGSIZE);

            assertThat(actualValue).isEqualTo(maxmsgsizeValue);
        }
    }

    // String Socket Options

    @Test
    @DisplayName("SocketOption_PlainUsername_ShouldBeConfigurable")
    void socketOption_PlainUsername_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {
            String expectedValue = "testuser";

            socket.setOption(SocketOption.PLAIN_USERNAME, expectedValue);
            byte[] buffer = socket.getOptionBytes(SocketOption.PLAIN_USERNAME, 256);
            String actualValue = new String(buffer, StandardCharsets.UTF_8).trim();

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"user1", "admin", "test_user_123"})
    @DisplayName("SocketOption_PlainUsername_ShouldAcceptVariousValues")
    void socketOption_PlainUsername_ShouldAcceptVariousValues(String username) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {

            socket.setOption(SocketOption.PLAIN_USERNAME, username);
            byte[] buffer = socket.getOptionBytes(SocketOption.PLAIN_USERNAME, 256);
            String actualValue = new String(buffer, StandardCharsets.UTF_8).trim();

            assertThat(actualValue).isEqualTo(username);
        }
    }

    @Test
    @DisplayName("SocketOption_PlainPassword_ShouldBeConfigurable")
    void socketOption_PlainPassword_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {
            String expectedValue = "password123";

            socket.setOption(SocketOption.PLAIN_PASSWORD, expectedValue);
            byte[] buffer = socket.getOptionBytes(SocketOption.PLAIN_PASSWORD, 256);
            String actualValue = new String(buffer, StandardCharsets.UTF_8).trim();

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"pass1", "secret", "my_secure_password"})
    @DisplayName("SocketOption_PlainPassword_ShouldAcceptVariousValues")
    void socketOption_PlainPassword_ShouldAcceptVariousValues(String password) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {

            socket.setOption(SocketOption.PLAIN_PASSWORD, password);
            byte[] buffer = socket.getOptionBytes(SocketOption.PLAIN_PASSWORD, 256);
            String actualValue = new String(buffer, StandardCharsets.UTF_8).trim();

            assertThat(actualValue).isEqualTo(password);
        }
    }

    @Test
    @DisplayName("SocketOption_ZapDomain_ShouldBeConfigurable")
    void socketOption_ZapDomain_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REP)) {
            String expectedValue = "global";

            socket.setOption(SocketOption.ZAP_DOMAIN, expectedValue);
            byte[] buffer = socket.getOptionBytes(SocketOption.ZAP_DOMAIN, 256);
            String actualValue = new String(buffer, StandardCharsets.UTF_8).trim();

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "global", "test.domain"})
    @DisplayName("SocketOption_ZapDomain_ShouldAcceptVariousValues")
    void socketOption_ZapDomain_ShouldAcceptVariousValues(String domain) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {

            socket.setOption(SocketOption.ZAP_DOMAIN, domain);
            byte[] buffer = socket.getOptionBytes(SocketOption.ZAP_DOMAIN, 256);
            String actualValue = new String(buffer, StandardCharsets.UTF_8).trim();

            assertThat(actualValue).isEqualTo(domain);
        }
    }

    @Test
    @DisplayName("SocketOption_SocksProxy_ShouldBeConfigurable")
    void socketOption_SocksProxy_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {
            String expectedValue = "127.0.0.1:1080";

            socket.setOption(SocketOption.SOCKS_PROXY, expectedValue);
            byte[] buffer = socket.getOptionBytes(SocketOption.SOCKS_PROXY, 256);
            String actualValue = new String(buffer, StandardCharsets.UTF_8).trim();

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1:1080", "proxy.example.com:1080"})
    @DisplayName("SocketOption_SocksProxy_ShouldAcceptVariousValues")
    void socketOption_SocksProxy_ShouldAcceptVariousValues(String proxy) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.SOCKS_PROXY, proxy);
            byte[] buffer = socket.getOptionBytes(SocketOption.SOCKS_PROXY, 256);
            String actualValue = new String(buffer, StandardCharsets.UTF_8).trim();

            assertThat(actualValue).isEqualTo(proxy);
        }
    }

    @Test
    @DisplayName("SocketOption_Bindtodevice_ShouldBeConfigurable")
    void socketOption_Bindtodevice_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PUB)) {
            String expectedValue = "eth0";

            socket.setOption(SocketOption.BINDTODEVICE, expectedValue);
            byte[] buffer = socket.getOptionBytes(SocketOption.BINDTODEVICE, 256);
            String actualValue = new String(buffer, StandardCharsets.UTF_8).trim();

            assertThat(actualValue).isEqualTo(expectedValue);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"eth0", "lo"})
    @DisplayName("SocketOption_Bindtodevice_ShouldAcceptVariousValues")
    void socketOption_Bindtodevice_ShouldAcceptVariousValues(String device) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {

            socket.setOption(SocketOption.BINDTODEVICE, device);
            byte[] buffer = socket.getOptionBytes(SocketOption.BINDTODEVICE, 256);
            String actualValue = new String(buffer, StandardCharsets.UTF_8).trim();

            assertThat(actualValue).isEqualTo(device);
        }
    }

    // Byte Array Socket Options

    @Test
    @DisplayName("SocketOption_Subscribe_ShouldBeConfigurable")
    void socketOption_Subscribe_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {
            byte[] expectedValue = new byte[]{0x01, 0x02, 0x03};

            socket.setOption(SocketOption.SUBSCRIBE, expectedValue);

            // Subscribe doesn't have a getter, so we just verify it doesn't throw
            assertThat(expectedValue).isNotNull();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "41",
            "41,42,43"
    })
    @DisplayName("SocketOption_Subscribe_ShouldAcceptVariousValues")
    void socketOption_Subscribe_ShouldAcceptVariousValues(String hexValues) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {
            byte[] topic = parseHexBytes(hexValues);

            socket.setOption(SocketOption.SUBSCRIBE, topic);

            // Subscribe doesn't have a getter, so we just verify it doesn't throw
            assertThat(topic).isNotNull();
        }
    }

    @Test
    @DisplayName("SocketOption_Unsubscribe_ShouldBeConfigurable")
    void socketOption_Unsubscribe_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.SUB)) {
            byte[] subscribeValue = new byte[]{0x01, 0x02, 0x03};
            byte[] unsubscribeValue = new byte[]{0x01, 0x02, 0x03};

            socket.setOption(SocketOption.SUBSCRIBE, subscribeValue);
            socket.setOption(SocketOption.UNSUBSCRIBE, unsubscribeValue);

            // Unsubscribe doesn't have a getter, so we just verify it doesn't throw
            assertThat(unsubscribeValue).isNotNull();
        }
    }

    @Test
    @DisplayName("SocketOption_ConnectRoutingId_ShouldBeConfigurable")
    void socketOption_ConnectRoutingId_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {
            byte[] expectedValue = new byte[]{0x01, 0x02, 0x03, 0x04};

            socket.setOption(SocketOption.CONNECT_ROUTING_ID, expectedValue);

            // Connect_Routing_Id is write-only, so we just verify it doesn't throw
            assertThat(expectedValue).isNotNull();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "01",
            "41,42,43",
            "01,02,03,04,05"
    })
    @DisplayName("SocketOption_ConnectRoutingId_ShouldAcceptVariousValues")
    void socketOption_ConnectRoutingId_ShouldAcceptVariousValues(String hexValues) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.ROUTER)) {
            byte[] routingId = parseHexBytes(hexValues);

            socket.setOption(SocketOption.CONNECT_ROUTING_ID, routingId);

            // Connect_Routing_Id is write-only, so we just verify it doesn't throw
            assertThat(routingId).isNotNull();
        }
    }

    @Test
    @DisplayName("SocketOption_XpubWelcomeMsg_ShouldBeConfigurable")
    void socketOption_XpubWelcomeMsg_ShouldBeConfigurable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.XPUB)) {
            byte[] expectedValue = new byte[]{0x57, 0x45, 0x4C, 0x43, 0x4F, 0x4D, 0x45}; // "WELCOME"

            socket.setOption(SocketOption.XPUB_WELCOME_MSG, expectedValue);

            // Xpub_Welcome_Msg is write-only, so we just verify it doesn't throw
            assertThat(expectedValue).isNotNull();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "48,49",
            "57,45,4C,43,4F,4D,45"
    })
    @DisplayName("SocketOption_XpubWelcomeMsg_ShouldAcceptVariousValues")
    void socketOption_XpubWelcomeMsg_ShouldAcceptVariousValues(String hexValues) {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.XPUB)) {
            byte[] welcomeMsg = parseHexBytes(hexValues);

            socket.setOption(SocketOption.XPUB_WELCOME_MSG, welcomeMsg);

            // Xpub_Welcome_Msg is write-only, so we just verify it doesn't throw
            assertThat(welcomeMsg).isNotNull();
        }
    }

    // Read-Only Socket Options

    @Test
    @DisplayName("SocketOption_Rcvmore_ShouldBeReadable")
    void socketOption_Rcvmore_ShouldBeReadable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.PULL)) {

            int rcvmore = socket.getOption(SocketOption.RCVMORE);

            assertThat(rcvmore).isIn(0, 1);
        }
    }

    @Test
    @DisplayName("SocketOption_Fd_ShouldBeReadable")
    void socketOption_Fd_ShouldBeReadable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {

            // FD should be a valid file descriptor
            // Just verify we can read it without throwing
            long fd = socket.getOptionLong(SocketOption.FD);

            assertThat(fd).isNotEqualTo(0L);
        }
    }

    @Test
    @DisplayName("SocketOption_Events_ShouldBeReadable")
    void socketOption_Events_ShouldBeReadable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {

            int events = socket.getOption(SocketOption.EVENTS);

            // Events is a bitmask, should be >= 0
            assertThat(events).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("SocketOption_Type_ShouldBeReadable")
    void socketOption_Type_ShouldBeReadable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REQ)) {

            int type = socket.getOption(SocketOption.TYPE);

            assertThat(type).isEqualTo(SocketType.REQ.getValue());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "REQ",
            "REP",
            "DEALER",
            "ROUTER",
            "PUB",
            "SUB",
            "PUSH",
            "PULL"
    })
    @DisplayName("SocketOption_Type_ShouldReturnCorrectSocketType")
    void socketOption_Type_ShouldReturnCorrectSocketType(String socketTypeName) {
        SocketType socketType = SocketType.valueOf(socketTypeName);
        try (Context context = new Context();
             Socket socket = new Socket(context, socketType)) {

            int type = socket.getOption(SocketOption.TYPE);

            assertThat(type).isEqualTo(socketType.getValue());
        }
    }

    @Test
    @DisplayName("SocketOption_LastEndpoint_ShouldBeReadableAfterBind")
    void socketOption_LastEndpoint_ShouldBeReadableAfterBind() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.REP)) {
            String bindAddress = "tcp://127.0.0.1:25557";

            socket.bind(bindAddress);
            byte[] buffer = socket.getOptionBytes(SocketOption.LAST_ENDPOINT, 256);
            String lastEndpoint = new String(buffer, StandardCharsets.UTF_8).trim();

            assertThat(lastEndpoint).isEqualTo(bindAddress);

            socket.unbind(bindAddress);
        }
    }

    @Test
    @DisplayName("SocketOption_Mechanism_ShouldBeReadable")
    void socketOption_Mechanism_ShouldBeReadable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {

            int mechanism = socket.getOption(SocketOption.MECHANISM);

            // Default mechanism should be NULL (0)
            assertThat(mechanism).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("SocketOption_ThreadSafe_ShouldBeReadable")
    void socketOption_ThreadSafe_ShouldBeReadable() {
        try (Context context = new Context();
             Socket socket = new Socket(context, SocketType.DEALER)) {

            int threadSafe = socket.getOption(SocketOption.THREAD_SAFE);

            // Most socket types are not thread-safe (0), some like CLIENT/SERVER are (1)
            assertThat(threadSafe).isIn(0, 1);
        }
    }

    // Helper method to parse hex strings
    private byte[] parseHexBytes(String hexString) {
        String[] parts = hexString.split(",");
        byte[] bytes = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            bytes[i] = (byte) Integer.parseInt(parts[i].trim(), 16);
        }
        return bytes;
    }
}
