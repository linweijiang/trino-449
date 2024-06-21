/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.block;

import io.trino.spi.block.Block;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.ByteArrayBlock;
import io.trino.spi.block.ByteArrayBlockBuilder;
import io.trino.spi.block.ShortArrayBlockBuilder;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class TestByteArrayBlock
        extends AbstractTestBlock
{
    @Test
    public void test()
    {
        Byte[] expectedValues = createTestValue(17);
        assertFixedWithValues(expectedValues);
        assertFixedWithValues(alternatingNullValues(expectedValues));
    }

    @Test
    public void testCopyPositions()
    {
        Byte[] expectedValues = alternatingNullValues(createTestValue(17));
        BlockBuilder blockBuilder = createBlockBuilderWithValues(expectedValues);
        assertBlockFilteredPositions(expectedValues, blockBuilder.build(), 0, 2, 4, 6, 7, 9, 10, 16);
    }

    @Test
    public void testLazyBlockBuilderInitialization()
    {
        Byte[] expectedValues = createTestValue(100);
        BlockBuilder emptyBlockBuilder = new ByteArrayBlockBuilder(null, 0);

        ByteArrayBlockBuilder blockBuilder = new ByteArrayBlockBuilder(null, expectedValues.length);
        assertThat(blockBuilder.getSizeInBytes()).isEqualTo(emptyBlockBuilder.getSizeInBytes());
        assertThat(blockBuilder.getRetainedSizeInBytes()).isEqualTo(emptyBlockBuilder.getRetainedSizeInBytes());

        writeValues(expectedValues, blockBuilder);
        assertThat(blockBuilder.getSizeInBytes() > emptyBlockBuilder.getSizeInBytes()).isTrue();
        assertThat(blockBuilder.getRetainedSizeInBytes() > emptyBlockBuilder.getRetainedSizeInBytes()).isTrue();

        blockBuilder = (ByteArrayBlockBuilder) blockBuilder.newBlockBuilderLike(null);
        assertThat(blockBuilder.getSizeInBytes()).isEqualTo(emptyBlockBuilder.getSizeInBytes());
        assertThat(blockBuilder.getRetainedSizeInBytes()).isEqualTo(emptyBlockBuilder.getRetainedSizeInBytes());
    }

    @Test
    public void testEstimatedDataSizeForStats()
    {
        BlockBuilder blockBuilder = createBlockBuilderWithValues(createTestValue(100));
        Block block = blockBuilder.build();
        for (int i = 0; i < block.getPositionCount(); i++) {
            assertThat(block.getEstimatedDataSizeForStats(i)).isEqualTo(Byte.BYTES);
        }

        assertThat(new ShortArrayBlockBuilder(null, 22).appendNull().build().getEstimatedDataSizeForStats(0)).isEqualTo(0);
    }

    @Test
    public void testCompactBlock()
    {
        byte[] byteArray = {(byte) 0, (byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4};
        boolean[] valueIsNull = {false, true, false, false, false, false};

        testCompactBlock(new ByteArrayBlock(0, Optional.empty(), new byte[0]));
        testCompactBlock(new ByteArrayBlock(byteArray.length, Optional.of(valueIsNull), byteArray));
        testNotCompactBlock(new ByteArrayBlock(byteArray.length - 1, Optional.of(valueIsNull), byteArray));
    }

    private void assertFixedWithValues(Byte[] expectedValues)
    {
        Block block = createBlockBuilderWithValues(expectedValues).build();
        assertBlock(block, expectedValues);
    }

    private static BlockBuilder createBlockBuilderWithValues(Byte[] expectedValues)
    {
        ByteArrayBlockBuilder blockBuilder = new ByteArrayBlockBuilder(null, expectedValues.length);
        writeValues(expectedValues, blockBuilder);
        return blockBuilder;
    }

    private static void writeValues(Byte[] expectedValues, ByteArrayBlockBuilder blockBuilder)
    {
        for (Byte expectedValue : expectedValues) {
            if (expectedValue == null) {
                blockBuilder.appendNull();
            }
            else {
                blockBuilder.writeByte(expectedValue);
            }
        }
    }

    private static Byte[] createTestValue(int positionCount)
    {
        Byte[] expectedValues = new Byte[positionCount];
        Random random = new Random(0);
        for (int position = 0; position < positionCount; position++) {
            expectedValues[position] = (byte) random.nextInt();
        }
        return expectedValues;
    }

    @Override
    protected <T> void assertPositionValue(Block block, int position, T expectedValue)
    {
        if (expectedValue == null) {
            assertThat(block.isNull(position)).isTrue();
            return;
        }

        assertThat(block.isNull(position)).isFalse();
        assertThat(((ByteArrayBlock) block).getByte(position)).isEqualTo(((Byte) expectedValue).byteValue());
    }
}
