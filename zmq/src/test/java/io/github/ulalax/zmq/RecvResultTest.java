package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for RecvResult class.
 */
@DisplayName("RecvResult")
class RecvResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("Should create success result with valid value")
        void should_Create_Success_Result_With_Valid_Value() {
            // When: Create success result with a string
            RecvResult<String> result = RecvResult.success("Hello");

            // Then: Result should be present and contain the value
            assertThat(result.isPresent()).isTrue();
            assertThat(result.wouldBlock()).isFalse();
            assertThat(result.value()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("Should create success result with byte array")
        void should_Create_Success_Result_With_Byte_Array() {
            // When: Create success result with byte array
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            RecvResult<byte[]> result = RecvResult.success(data);

            // Then: Result should be present and contain the value
            assertThat(result.isPresent()).isTrue();
            assertThat(result.value()).isEqualTo(data);
        }

        @Test
        @DisplayName("Should create success result with integer")
        void should_Create_Success_Result_With_Integer() {
            // When: Create success result with integer
            RecvResult<Integer> result = RecvResult.success(42);

            // Then: Result should be present and contain the value
            assertThat(result.isPresent()).isTrue();
            assertThat(result.value()).isEqualTo(42);
        }

        @Test
        @DisplayName("Should throw NullPointerException when creating success with null value")
        void should_Throw_NullPointerException_When_Creating_Success_With_Null_Value() {
            // When & Then: Create success result with null should throw
            assertThatThrownBy(() -> RecvResult.success(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("Should create would-block result")
        void should_Create_Would_Block_Result() {
            // When: Create would-block result
            RecvResult<String> result = RecvResult.empty();

            // Then: Result should not be present and should be would-block
            assertThat(result.isPresent()).isFalse();
            assertThat(result.wouldBlock()).isTrue();
        }

        @Test
        @DisplayName("Should create typed would-block result")
        void should_Create_Typed_Would_Block_Result() {
            // When: Create typed would-block results
            RecvResult<String> stringResult = RecvResult.empty();
            RecvResult<byte[]> bytesResult = RecvResult.empty();
            RecvResult<Integer> intResult = RecvResult.empty();

            // Then: All should be would-block
            assertThat(stringResult.wouldBlock()).isTrue();
            assertThat(bytesResult.wouldBlock()).isTrue();
            assertThat(intResult.wouldBlock()).isTrue();
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        @Test
        @DisplayName("isPresent should return true for success result")
        void isPresent_Should_Return_True_For_Success_Result() {
            // Given: A success result
            RecvResult<String> result = RecvResult.success("data");

            // When & Then: isPresent should return true
            assertThat(result.isPresent()).isTrue();
        }

        @Test
        @DisplayName("isPresent should return false for would-block result")
        void isPresent_Should_Return_False_For_Would_Block_Result() {
            // Given: A would-block result
            RecvResult<String> result = RecvResult.empty();

            // When & Then: isPresent should return false
            assertThat(result.isPresent()).isFalse();
        }

        @Test
        @DisplayName("wouldBlock should return false for success result")
        void wouldBlock_Should_Return_False_For_Success_Result() {
            // Given: A success result
            RecvResult<Integer> result = RecvResult.success(100);

            // When & Then: wouldBlock should return false
            assertThat(result.wouldBlock()).isFalse();
        }

        @Test
        @DisplayName("wouldBlock should return true for would-block result")
        void wouldBlock_Should_Return_True_For_Would_Block_Result() {
            // Given: A would-block result
            RecvResult<Integer> result = RecvResult.empty();

            // When & Then: wouldBlock should return true
            assertThat(result.wouldBlock()).isTrue();
        }
    }

    @Nested
    @DisplayName("Value Retrieval")
    class ValueRetrieval {

        @Test
        @DisplayName("value should return received value for success result")
        void value_Should_Return_Received_Value_For_Success_Result() {
            // Given: A success result with string
            RecvResult<String> result = RecvResult.success("test message");

            // When: Get value
            String value = result.value();

            // Then: Should return the string
            assertThat(value).isEqualTo("test message");
        }

        @Test
        @DisplayName("value should throw NoSuchElementException for would-block result")
        void value_Should_Throw_NoSuchElementException_For_Would_Block_Result() {
            // Given: A would-block result
            RecvResult<String> result = RecvResult.empty();

            // When & Then: Getting value should throw
            assertThatThrownBy(result::value)
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("empty")
                    .hasMessageContaining("would block");
        }

        @Test
        @DisplayName("orElse should return value for success result")
        void orElse_Should_Return_Value_For_Success_Result() {
            // Given: A success result
            RecvResult<String> result = RecvResult.success("actual");

            // When: Get value with default
            String value = result.orElse("default");

            // Then: Should return actual value, not default
            assertThat(value).isEqualTo("actual");
        }

        @Test
        @DisplayName("orElse should return default for would-block result")
        void orElse_Should_Return_Default_For_Would_Block_Result() {
            // Given: A would-block result
            RecvResult<String> result = RecvResult.empty();

            // When: Get value with default
            String value = result.orElse("default");

            // Then: Should return default value
            assertThat(value).isEqualTo("default");
        }

        @Test
        @DisplayName("orElse should accept null as default value")
        void orElse_Should_Accept_Null_As_Default_Value() {
            // Given: A would-block result
            RecvResult<String> result = RecvResult.empty();

            // When: Get value with null default
            String value = result.orElse(null);

            // Then: Should return null
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("orElseThrow should return value for success result")
        void orElseThrow_Should_Return_Value_For_Success_Result() {
            // Given: A success result
            RecvResult<byte[]> result = RecvResult.success(new byte[]{1, 2, 3});

            // When: Get value or throw
            byte[] value = result.orElseThrow();

            // Then: Should return value without throwing
            assertThat(value).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("orElseThrow should throw NoSuchElementException for would-block result")
        void orElseThrow_Should_Throw_NoSuchElementException_For_Would_Block_Result() {
            // Given: A would-block result
            RecvResult<String> result = RecvResult.empty();

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
            RecvResult<String> result = RecvResult.success("hello");
            AtomicReference<String> captured = new AtomicReference<>();

            // When: Execute ifPresent
            result.ifPresent(captured::set);

            // Then: Action should have been executed with the value
            assertThat(captured.get()).isEqualTo("hello");
        }

        @Test
        @DisplayName("ifPresent should not execute action for would-block result")
        void ifPresent_Should_Not_Execute_Action_For_Would_Block_Result() {
            // Given: A would-block result and an action
            RecvResult<String> result = RecvResult.empty();
            AtomicReference<String> captured = new AtomicReference<>("initial");

            // When: Execute ifPresent
            result.ifPresent(captured::set);

            // Then: Action should not have been executed
            assertThat(captured.get()).isEqualTo("initial");
        }

        @Test
        @DisplayName("ifPresent should throw NullPointerException when action is null for success result")
        void ifPresent_Should_Throw_NullPointerException_When_Action_Is_Null_For_Success_Result() {
            // Given: A success result
            RecvResult<String> result = RecvResult.success("value");

            // When & Then: ifPresent with null action should throw
            assertThatThrownBy(() -> result.ifPresent(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("action cannot be null");
        }

        @Test
        @DisplayName("ifPresent should not throw when action is null for would-block result")
        void ifPresent_Should_Not_Throw_When_Action_Is_Null_For_Would_Block_Result() {
            // Given: A would-block result
            RecvResult<String> result = RecvResult.empty();

            // When & Then: ifPresent with null action should not throw
            assertThatCode(() -> result.ifPresent(null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Map Transformation")
    class MapTransformation {

        @Test
        @DisplayName("map should transform value for success result")
        void map_Should_Transform_Value_For_Success_Result() {
            // Given: A success result with string
            RecvResult<String> result = RecvResult.success("hello");

            // When: Map to uppercase
            RecvResult<String> mapped = result.map(String::toUpperCase);

            // Then: Should contain transformed value
            assertThat(mapped.isPresent()).isTrue();
            assertThat(mapped.value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("map should transform to different type")
        void map_Should_Transform_To_Different_Type() {
            // Given: A success result with byte array
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            RecvResult<byte[]> result = RecvResult.success(data);

            // When: Map to length (Integer)
            RecvResult<Integer> mapped = result.map(bytes -> bytes.length);

            // Then: Should contain length as Integer
            assertThat(mapped.isPresent()).isTrue();
            assertThat(mapped.value()).isEqualTo(4);
        }

        @Test
        @DisplayName("map should return would-block for would-block result")
        void map_Should_Return_Would_Block_For_Would_Block_Result() {
            // Given: A would-block result
            RecvResult<String> result = RecvResult.empty();

            // When: Map with any function
            RecvResult<Integer> mapped = result.map(String::length);

            // Then: Should still be would-block (mapper not executed)
            assertThat(mapped.wouldBlock()).isTrue();
            assertThat(mapped.isPresent()).isFalse();
        }

        @Test
        @DisplayName("map should throw NullPointerException when mapper is null")
        void map_Should_Throw_NullPointerException_When_Mapper_Is_Null() {
            // Given: A success result
            RecvResult<String> result = RecvResult.success("value");

            // When & Then: Map with null mapper should throw
            assertThatThrownBy(() -> result.map(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("mapper cannot be null");
        }

        @Test
        @DisplayName("map should throw NullPointerException when mapper returns null")
        void map_Should_Throw_NullPointerException_When_Mapper_Returns_Null() {
            // Given: A success result
            RecvResult<String> result = RecvResult.success("value");

            // When & Then: Map that returns null should throw
            assertThatThrownBy(() -> result.map(s -> null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not return null");
        }

        @Test
        @DisplayName("map should support chaining multiple transformations")
        void map_Should_Support_Chaining_Multiple_Transformations() {
            // Given: A success result with string
            RecvResult<String> result = RecvResult.success("  hello world  ");

            // When: Chain multiple transformations
            RecvResult<Integer> mapped = result
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(String::length);

            // Then: Should apply all transformations
            assertThat(mapped.isPresent()).isTrue();
            assertThat(mapped.value()).isEqualTo(11); // "HELLO WORLD".length()
        }

        @Test
        @DisplayName("map should preserve would-block through chain")
        void map_Should_Preserve_Would_Block_Through_Chain() {
            // Given: A would-block result
            RecvResult<String> result = RecvResult.empty();

            // When: Chain multiple transformations
            RecvResult<Integer> mapped = result
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(String::length);

            // Then: Should remain would-block (no mapper executed)
            assertThat(mapped.wouldBlock()).isTrue();
        }

        @Test
        @DisplayName("map should handle complex transformations")
        void map_Should_Handle_Complex_Transformations() {
            // Given: A success result with byte array
            byte[] data = new byte[]{1, 2, 3, 4, 5};
            RecvResult<byte[]> result = RecvResult.success(data);

            // When: Map to sum of bytes
            RecvResult<Integer> sum = result.map(bytes -> {
                int total = 0;
                for (byte b : bytes) {
                    total += b;
                }
                return total;
            });

            // Then: Should calculate sum correctly
            assertThat(sum.isPresent()).isTrue();
            assertThat(sum.value()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityAndHashCode {

        @Test
        @DisplayName("Should be equal to itself")
        void should_Be_Equal_To_Itself() {
            // Given: A result
            RecvResult<String> result = RecvResult.success("test");

            // When & Then: Should equal itself
            assertThat(result).isEqualTo(result);
        }

        @Test
        @DisplayName("Should be equal to another result with same value")
        void should_Be_Equal_To_Another_Result_With_Same_Value() {
            // Given: Two success results with same value
            RecvResult<String> result1 = RecvResult.success("test");
            RecvResult<String> result2 = RecvResult.success("test");

            // When & Then: Should be equal
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to result with different value")
        void should_Not_Be_Equal_To_Result_With_Different_Value() {
            // Given: Two success results with different values
            RecvResult<String> result1 = RecvResult.success("test1");
            RecvResult<String> result2 = RecvResult.success("test2");

            // When & Then: Should not be equal
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("Should be equal to another would-block result")
        void should_Be_Equal_To_Another_Would_Block_Result() {
            // Given: Two would-block results
            RecvResult<String> result1 = RecvResult.empty();
            RecvResult<String> result2 = RecvResult.empty();

            // When & Then: Should be equal
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to would-block result")
        void should_Not_Be_Equal_To_Would_Block_Result() {
            // Given: A success result and a would-block result
            RecvResult<String> success = RecvResult.success("data");
            RecvResult<String> wouldBlock = RecvResult.empty();

            // When & Then: Should not be equal
            assertThat(success).isNotEqualTo(wouldBlock);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void should_Not_Be_Equal_To_Null() {
            // Given: A result
            RecvResult<String> result = RecvResult.success("test");

            // When & Then: Should not equal null
            assertThat(result).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to object of different class")
        void should_Not_Be_Equal_To_Object_Of_Different_Class() {
            // Given: A result and a different object
            RecvResult<String> result = RecvResult.success("test");
            Object other = "not a RecvResult";

            // When & Then: Should not be equal
            assertThat(result).isNotEqualTo(other);
        }

        @Test
        @DisplayName("Should have consistent hashCode for success results")
        void should_Have_Consistent_HashCode_For_Success_Results() {
            // Given: A success result
            RecvResult<String> result = RecvResult.success("test");

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
            RecvResult<Integer> result = RecvResult.empty();

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
        @DisplayName("toString should show value for success result")
        void toString_Should_Show_Value_For_Success_Result() {
            // Given: A success result with string
            RecvResult<String> result = RecvResult.success("test message");

            // When: Convert to string
            String str = result.toString();

            // Then: Should contain "RecvResult" and the value
            assertThat(str)
                    .contains("RecvResult")
                    .contains("test message");
        }

        @Test
        @DisplayName("toString should show WOULD_BLOCK for would-block result")
        void toString_Should_Show_WOULD_BLOCK_For_Would_Block_Result() {
            // Given: A would-block result
            RecvResult<String> result = RecvResult.empty();

            // When: Convert to string
            String str = result.toString();

            // Then: Should contain "RecvResult" and "WOULD_BLOCK"
            assertThat(str)
                    .contains("RecvResult")
                    .contains("WOULD_BLOCK");
        }

        @Test
        @DisplayName("toString should be distinct for success and would-block")
        void toString_Should_Be_Distinct_For_Success_And_Would_Block() {
            // Given: A success and a would-block result
            RecvResult<String> success = RecvResult.success("data");
            RecvResult<String> wouldBlock = RecvResult.empty();

            // When: Convert to strings
            String successStr = success.toString();
            String wouldBlockStr = wouldBlock.toString();

            // Then: Strings should be different
            assertThat(successStr).isNotEqualTo(wouldBlockStr);
        }

        @Test
        @DisplayName("toString should handle different value types")
        void toString_Should_Handle_Different_Value_Types() {
            // Given: Results with different types
            RecvResult<String> stringResult = RecvResult.success("hello");
            RecvResult<Integer> intResult = RecvResult.success(42);

            // When: Convert to strings
            String stringStr = stringResult.toString();
            String intStr = intResult.toString();

            // Then: Should show appropriate representations
            assertThat(stringStr).contains("hello");
            assertThat(intStr).contains("42");
        }
    }

    @Nested
    @DisplayName("Generic Type Safety")
    class GenericTypeSafety {

        @Test
        @DisplayName("Should maintain type safety with String")
        void should_Maintain_Type_Safety_With_String() {
            // Given: A string result
            RecvResult<String> result = RecvResult.success("test");

            // When: Get value
            String value = result.value();

            // Then: Type should be String
            assertThat(value).isInstanceOf(String.class);
        }

        @Test
        @DisplayName("Should maintain type safety with byte array")
        void should_Maintain_Type_Safety_With_Byte_Array() {
            // Given: A byte array result
            byte[] data = new byte[]{1, 2, 3};
            RecvResult<byte[]> result = RecvResult.success(data);

            // When: Get value
            byte[] value = result.value();

            // Then: Type should be byte array
            assertThat(value).isInstanceOf(byte[].class);
        }

        @Test
        @DisplayName("Should maintain type safety with Integer")
        void should_Maintain_Type_Safety_With_Integer() {
            // Given: An integer result
            RecvResult<Integer> result = RecvResult.success(100);

            // When: Get value
            Integer value = result.value();

            // Then: Type should be Integer
            assertThat(value).isInstanceOf(Integer.class);
        }

        @Test
        @DisplayName("Should maintain type safety through map transformations")
        void should_Maintain_Type_Safety_Through_Map_Transformations() {
            // Given: A string result
            RecvResult<String> stringResult = RecvResult.success("test");

            // When: Transform to integer
            RecvResult<Integer> intResult = stringResult.map(String::length);

            // Then: Type should be Integer
            assertThat(intResult.value()).isInstanceOf(Integer.class);
        }
    }

    @Nested
    @DisplayName("Usage Patterns")
    class UsagePatterns {

        @Test
        @DisplayName("Should support typical non-blocking receive pattern")
        void should_Support_Typical_Non_Blocking_Receive_Pattern() {
            // Given: A receive result
            RecvResult<String> result = RecvResult.success("message");

            // When: Check and use value (typical pattern)
            if (result.isPresent()) {
                String msg = result.value();
                // Then: Should work as expected
                assertThat(msg).isEqualTo("message");
            } else {
                fail("Expected result to be present");
            }
        }

        @Test
        @DisplayName("Should support would-block handling pattern")
        void should_Support_Would_Block_Handling_Pattern() {
            // Given: A would-block result
            RecvResult<byte[]> result = RecvResult.empty();

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
            RecvResult<String> result = RecvResult.success("data");
            AtomicReference<String> processed = new AtomicReference<>();

            // When: Use functional pattern
            result.ifPresent(msg -> processed.set(msg.toUpperCase()));

            // Then: Should process correctly
            assertThat(processed.get()).isEqualTo("DATA");
        }

        @Test
        @DisplayName("Should support default value pattern")
        void should_Support_Default_Value_Pattern() {
            // Given: Would-block result
            RecvResult<String> result = RecvResult.empty();

            // When: Use default value pattern
            String msg = result.orElse("no data");

            // Then: Should use default
            assertThat(msg).isEqualTo("no data");
        }

        @Test
        @DisplayName("Should support transformation pipeline pattern")
        void should_Support_Transformation_Pipeline_Pattern() {
            // Given: A byte array result
            byte[] data = "  test  ".getBytes(StandardCharsets.UTF_8);
            RecvResult<byte[]> result = RecvResult.success(data);

            // When: Build transformation pipeline
            RecvResult<Integer> length = result
                    .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                    .map(String::trim)
                    .map(String::length);

            // Then: Should process through pipeline
            assertThat(length.value()).isEqualTo(4);
        }
    }
}
