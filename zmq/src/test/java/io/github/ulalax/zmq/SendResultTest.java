package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for SendResult class.
 */
@DisplayName("SendResult")
class SendResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("Should create success result with valid bytes sent")
        void should_Create_Success_Result_With_Valid_Bytes_Sent() {
            // When: Create success result with 100 bytes
            SendResult result = SendResult.success(100);

            // Then: Result should be present and contain the value
            assertThat(result.isPresent()).isTrue();
            assertThat(result.wouldBlock()).isFalse();
            assertThat(result.value()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should create success result with zero bytes")
        void should_Create_Success_Result_With_Zero_Bytes() {
            // When: Create success result with 0 bytes
            SendResult result = SendResult.success(0);

            // Then: Result should be present with value 0
            assertThat(result.isPresent()).isTrue();
            assertThat(result.value()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should throw exception when creating success with negative bytes")
        void should_Throw_Exception_When_Creating_Success_With_Negative_Bytes() {
            // When & Then: Create success result with negative bytes should throw
            assertThatThrownBy(() -> SendResult.success(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be non-negative");
        }

        @Test
        @DisplayName("Should create would-block result")
        void should_Create_Would_Block_Result() {
            // When: Create would-block result
            SendResult result = SendResult.empty();

            // Then: Result should not be present and should be would-block
            assertThat(result.isPresent()).isFalse();
            assertThat(result.wouldBlock()).isTrue();
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        @Test
        @DisplayName("isPresent should return true for success result")
        void isPresent_Should_Return_True_For_Success_Result() {
            // Given: A success result
            SendResult result = SendResult.success(42);

            // When & Then: isPresent should return true
            assertThat(result.isPresent()).isTrue();
        }

        @Test
        @DisplayName("isPresent should return false for would-block result")
        void isPresent_Should_Return_False_For_Would_Block_Result() {
            // Given: A would-block result
            SendResult result = SendResult.empty();

            // When & Then: isPresent should return false
            assertThat(result.isPresent()).isFalse();
        }

        @Test
        @DisplayName("wouldBlock should return false for success result")
        void wouldBlock_Should_Return_False_For_Success_Result() {
            // Given: A success result
            SendResult result = SendResult.success(42);

            // When & Then: wouldBlock should return false
            assertThat(result.wouldBlock()).isFalse();
        }

        @Test
        @DisplayName("wouldBlock should return true for would-block result")
        void wouldBlock_Should_Return_True_For_Would_Block_Result() {
            // Given: A would-block result
            SendResult result = SendResult.empty();

            // When & Then: wouldBlock should return true
            assertThat(result.wouldBlock()).isTrue();
        }
    }

    @Nested
    @DisplayName("Value Retrieval")
    class ValueRetrieval {

        @Test
        @DisplayName("value should return bytes sent for success result")
        void value_Should_Return_Bytes_Sent_For_Success_Result() {
            // Given: A success result with 256 bytes
            SendResult result = SendResult.success(256);

            // When: Get value
            int bytes = result.value();

            // Then: Should return 256
            assertThat(bytes).isEqualTo(256);
        }

        @Test
        @DisplayName("value should throw NoSuchElementException for would-block result")
        void value_Should_Throw_NoSuchElementException_For_Would_Block_Result() {
            // Given: A would-block result
            SendResult result = SendResult.empty();

            // When & Then: Getting value should throw
            assertThatThrownBy(result::value)
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("empty")
                    .hasMessageContaining("would block");
        }

        @Test
        @DisplayName("orElse should return value for success result")
        void orElse_Should_Return_Value_For_Success_Result() {
            // Given: A success result with 100 bytes
            SendResult result = SendResult.success(100);

            // When: Get value with default
            int bytes = result.orElse(0);

            // Then: Should return actual value, not default
            assertThat(bytes).isEqualTo(100);
        }

        @Test
        @DisplayName("orElse should return default for would-block result")
        void orElse_Should_Return_Default_For_Would_Block_Result() {
            // Given: A would-block result
            SendResult result = SendResult.empty();

            // When: Get value with default
            int bytes = result.orElse(-1);

            // Then: Should return default value
            assertThat(bytes).isEqualTo(-1);
        }

        @Test
        @DisplayName("orElseThrow should return value for success result")
        void orElseThrow_Should_Return_Value_For_Success_Result() {
            // Given: A success result with 512 bytes
            SendResult result = SendResult.success(512);

            // When: Get value or throw
            int bytes = result.orElseThrow();

            // Then: Should return value without throwing
            assertThat(bytes).isEqualTo(512);
        }

        @Test
        @DisplayName("orElseThrow should throw NoSuchElementException for would-block result")
        void orElseThrow_Should_Throw_NoSuchElementException_For_Would_Block_Result() {
            // Given: A would-block result
            SendResult result = SendResult.empty();

            // When & Then: orElseThrow should throw
            assertThatThrownBy(result::orElseThrow)
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("empty");
        }
    }

    @Nested
    @DisplayName("Functional Methods")
    class FunctionalMethods {

        @Test
        @DisplayName("ifPresent should execute action for success result")
        void ifPresent_Should_Execute_Action_For_Success_Result() {
            // Given: A success result and an action
            SendResult result = SendResult.success(123);
            AtomicInteger captured = new AtomicInteger(0);

            // When: Execute ifPresent
            result.ifPresent(captured::set);

            // Then: Action should have been executed with the value
            assertThat(captured.get()).isEqualTo(123);
        }

        @Test
        @DisplayName("ifPresent should not execute action for would-block result")
        void ifPresent_Should_Not_Execute_Action_For_Would_Block_Result() {
            // Given: A would-block result and an action
            SendResult result = SendResult.empty();
            AtomicInteger captured = new AtomicInteger(0);

            // When: Execute ifPresent
            result.ifPresent(captured::set);

            // Then: Action should not have been executed
            assertThat(captured.get()).isEqualTo(0);
        }

        @Test
        @DisplayName("ifPresent should throw NullPointerException when action is null for success result")
        void ifPresent_Should_Throw_NullPointerException_When_Action_Is_Null_For_Success_Result() {
            // Given: A success result
            SendResult result = SendResult.success(100);

            // When & Then: ifPresent with null action should throw
            assertThatThrownBy(() -> result.ifPresent(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("action cannot be null");
        }

        @Test
        @DisplayName("ifPresent should not throw when action is null for would-block result")
        void ifPresent_Should_Not_Throw_When_Action_Is_Null_For_Would_Block_Result() {
            // Given: A would-block result
            SendResult result = SendResult.empty();

            // When & Then: ifPresent with null action should not throw (action not executed)
            assertThatCode(() -> result.ifPresent(null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityAndHashCode {

        @Test
        @DisplayName("Should be equal to itself")
        void should_Be_Equal_To_Itself() {
            // Given: A result
            SendResult result = SendResult.success(100);

            // When & Then: Should equal itself
            assertThat(result).isEqualTo(result);
        }

        @Test
        @DisplayName("Should be equal to another result with same value")
        void should_Be_Equal_To_Another_Result_With_Same_Value() {
            // Given: Two success results with same value
            SendResult result1 = SendResult.success(100);
            SendResult result2 = SendResult.success(100);

            // When & Then: Should be equal
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to result with different value")
        void should_Not_Be_Equal_To_Result_With_Different_Value() {
            // Given: Two success results with different values
            SendResult result1 = SendResult.success(100);
            SendResult result2 = SendResult.success(200);

            // When & Then: Should not be equal
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("Should be equal to another would-block result")
        void should_Be_Equal_To_Another_Would_Block_Result() {
            // Given: Two would-block results
            SendResult result1 = SendResult.empty();
            SendResult result2 = SendResult.empty();

            // When & Then: Should be equal
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to would-block result")
        void should_Not_Be_Equal_To_Would_Block_Result() {
            // Given: A success result and a would-block result
            SendResult success = SendResult.success(100);
            SendResult wouldBlock = SendResult.empty();

            // When & Then: Should not be equal
            assertThat(success).isNotEqualTo(wouldBlock);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void should_Not_Be_Equal_To_Null() {
            // Given: A result
            SendResult result = SendResult.success(100);

            // When & Then: Should not equal null
            assertThat(result).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to object of different class")
        void should_Not_Be_Equal_To_Object_Of_Different_Class() {
            // Given: A result and a different object
            SendResult result = SendResult.success(100);
            Object other = "not a SendResult";

            // When & Then: Should not be equal
            assertThat(result).isNotEqualTo(other);
        }

        @Test
        @DisplayName("Should have consistent hashCode for success results")
        void should_Have_Consistent_HashCode_For_Success_Results() {
            // Given: A success result
            SendResult result = SendResult.success(42);

            // When: Get hashCode multiple times
            int hash1 = result.hashCode();
            int hash2 = result.hashCode();

            // Then: HashCode should be consistent
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("Should have consistent hashCode for would-block results")
        void should_Have_Consistent_HashCode_For_Would_Block_Results() {
            // Given: A would-block result
            SendResult result = SendResult.empty();

            // When: Get hashCode multiple times
            int hash1 = result.hashCode();
            int hash2 = result.hashCode();

            // Then: HashCode should be consistent
            assertThat(hash1).isEqualTo(hash2);
        }
    }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {

        @Test
        @DisplayName("toString should show bytes for success result")
        void toString_Should_Show_Bytes_For_Success_Result() {
            // Given: A success result with 256 bytes
            SendResult result = SendResult.success(256);

            // When: Convert to string
            String str = result.toString();

            // Then: Should contain "SendResult" and the value
            assertThat(str)
                    .contains("SendResult")
                    .contains("256")
                    .contains("bytes");
        }

        @Test
        @DisplayName("toString should show WOULD_BLOCK for would-block result")
        void toString_Should_Show_WOULD_BLOCK_For_Would_Block_Result() {
            // Given: A would-block result
            SendResult result = SendResult.empty();

            // When: Convert to string
            String str = result.toString();

            // Then: Should contain "SendResult" and "WOULD_BLOCK"
            assertThat(str)
                    .contains("SendResult")
                    .contains("WOULD_BLOCK");
        }

        @Test
        @DisplayName("toString should be distinct for success and would-block")
        void toString_Should_Be_Distinct_For_Success_And_Would_Block() {
            // Given: A success and a would-block result
            SendResult success = SendResult.success(100);
            SendResult wouldBlock = SendResult.empty();

            // When: Convert to strings
            String successStr = success.toString();
            String wouldBlockStr = wouldBlock.toString();

            // Then: Strings should be different
            assertThat(successStr).isNotEqualTo(wouldBlockStr);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle large byte values")
        void should_Handle_Large_Byte_Values() {
            // Given: A success result with large value
            SendResult result = SendResult.success(Integer.MAX_VALUE);

            // When & Then: Should handle correctly
            assertThat(result.isPresent()).isTrue();
            assertThat(result.value()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("Should handle zero bytes distinctly from would-block")
        void should_Handle_Zero_Bytes_Distinctly_From_Would_Block() {
            // Given: A success result with 0 bytes and a would-block result
            SendResult zero = SendResult.success(0);
            SendResult wouldBlock = SendResult.empty();

            // When & Then: They should be different
            assertThat(zero.isPresent()).isTrue();
            assertThat(wouldBlock.isPresent()).isFalse();
            assertThat(zero).isNotEqualTo(wouldBlock);
        }

        @Test
        @DisplayName("Should be usable in chained calls")
        void should_Be_Usable_In_Chained_Calls() {
            // Given: A success result
            SendResult result = SendResult.success(100);

            // When: Chain multiple operations
            int finalValue = result
                    .orElse(0);

            // Then: Should work correctly
            assertThat(finalValue).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Usage Patterns")
    class UsagePatterns {

        @Test
        @DisplayName("Should support typical non-blocking send pattern")
        void should_Support_Typical_Non_Blocking_Send_Pattern() {
            // Given: A send result
            SendResult result = SendResult.success(42);

            // When: Check and use value (typical pattern)
            if (result.isPresent()) {
                int bytes = result.value();
                // Then: Should work as expected
                assertThat(bytes).isEqualTo(42);
            } else {
                fail("Expected result to be present");
            }
        }

        @Test
        @DisplayName("Should support would-block handling pattern")
        void should_Support_Would_Block_Handling_Pattern() {
            // Given: A would-block result
            SendResult result = SendResult.empty();

            // When: Check for would-block
            boolean needsRetry = false;
            if (result.wouldBlock()) {
                needsRetry = true;
            }

            // Then: Should identify would-block correctly
            assertThat(needsRetry).isTrue();
        }

        @Test
        @DisplayName("Should support functional pattern with ifPresent")
        void should_Support_Functional_Pattern_With_ifPresent() {
            // Given: A success result
            SendResult result = SendResult.success(100);
            AtomicInteger sum = new AtomicInteger(0);

            // When: Use functional pattern
            result.ifPresent(bytes -> sum.addAndGet(bytes));

            // Then: Should accumulate correctly
            assertThat(sum.get()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should support default value pattern")
        void should_Support_Default_Value_Pattern() {
            // Given: Would-block result
            SendResult result = SendResult.empty();

            // When: Use default value pattern
            int bytes = result.orElse(0);

            // Then: Should use default
            assertThat(bytes).isEqualTo(0);
        }
    }
}
