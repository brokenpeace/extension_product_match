package org.datacleaner.extension.productmatch;

import static org.junit.Assert.assertEquals;

import org.datacleaner.api.InputColumn;
import org.datacleaner.data.MockInputColumn;
import org.datacleaner.data.MockInputRow;
import org.junit.Test;

import cern.colt.Arrays;

public class ProductMatchTransformerTest {

    private final MockInputColumn<String> gtin = new MockInputColumn<>("gtin");
    private final MockInputColumn<String> product = new MockInputColumn<>("product");
    private final MockInputColumn<String> brand = new MockInputColumn<>("brand");
    private final MockInputColumn<String> description1 = new MockInputColumn<>("description1");
    private final MockInputColumn<String> description2 = new MockInputColumn<>("description2");

    @Test
    public void testPlainTextMatchCocaCola() throws Exception {
        final ProductMatchTransformer transformer = createTransformer(description1, description2);

        Object[] result = transformer
                .transform(new MockInputRow().put(description1, "Coca-cola").put(description2, "2"));
        assertEquals(
                "[GOOD_MATCH, 7.8549566, 7894900011517, Coca Cola 2 litros||Refrigerantes | COCA COLA 2 LTRS, Coca-Cola, 5MRM4M, Food/Beverage/Tobacco, null, null, null]",
                Arrays.toString(result));

        result = transformer.transform(new MockInputRow().put(description1, "Coca cola zero 1 liter"));
        assertEquals(
                "[POTENTIAL_MATCH, 4.9360476, 7894900701753, COCA COLA ZERO 1,, Coca-Cola, 5MRM4M, Food/Beverage/Tobacco, null, null, null]",
                Arrays.toString(result));
    }

    @Test
    public void testMatchOnProductAndBrandCocaCola() throws Exception {

        final ProductMatchTransformer transformer = createTransformer(product, brand);

        Object[] result = transformer.transform(new MockInputRow().put(brand, "Coca-cola").put(product, "Free"));
        assertEquals(
                "[GOOD_MATCH, 8.123512, 0049000006131, Caffeine Free, Coca-Cola, 5MRM4M, Food/Beverage/Tobacco, null, null, null]",
                Arrays.toString(result));

        result = transformer.transform(new MockInputRow().put(brand, "Coca-cola").put(product, "Life"));
        assertEquals(
                "[POTENTIAL_MATCH, 3.0357375, 0049000000061, Cola With Cherry Flavor, Coca-Cola, 5MRM4M, Food/Beverage/Tobacco, null, null, null]",
                Arrays.toString(result));
    }

    @Test
    public void testMatchOnProductAndBrandLego() throws Exception {
        final ProductMatchTransformer transformer = createTransformer(product, brand, description1);

        Object[] result = transformer
                .transform(new MockInputRow().put(brand, "Lego").put(product, "Star wars destroyer").put(description1,
                        "The elefant-like thing from the Star Wars movies"));
        assertEquals(
                "[POTENTIAL_MATCH, 6.065925, 0082493500007, Star Wars Imperial Star Destroyer, Lego, GSD9GK, Toys/Games, null, null, null]",
                Arrays.toString(result));
    }

    @Test
    public void testBadMatchOnProductAndBrandLego() throws Exception {
        final ProductMatchTransformer transformer = createTransformer(product, brand, description1);

        Object[] result = transformer
                .transform(new MockInputRow().put(brand, "Lego").put(product, "Hello world").put(description1,
                        "I am looking for something quite different than this"));
        assertEquals(
                "[NO_MATCH, null, null, Hello world, Lego, null, null, null, null, null]",
                Arrays.toString(result));
    }

    @Test
    public void testNoMatchUnknownTerms() throws Exception {
        final ProductMatchTransformer transformer = createTransformer(description1);

        Object[] result = transformer.transform(new MockInputRow().put(description1, "helloworldabracadabra"));
        assertEquals("[NO_MATCH, null, null, null, null, null, null, null, null, null]", Arrays.toString(result));
    }

    @Test
    public void testNoMatchNoQuery() throws Exception {
        final ProductMatchTransformer transformer = createTransformer(description1);

        Object[] result = transformer.transform(new MockInputRow().put(description1, ""));
        assertEquals("[SKIPPED, null, null, null, null, null, null, null, null, null]", Arrays.toString(result));

        result = transformer.transform(new MockInputRow().put(description1, null));
        assertEquals("[SKIPPED, null, null, null, null, null, null, null, null, null]", Arrays.toString(result));
    }

    @Test
    public void testGtinLookup() throws Exception {
        final ProductMatchTransformer transformer = createTransformer(gtin);

        Object[] result = transformer.transform(new MockInputRow().put(gtin, "0300743288131"));
        assertEquals(
                "[GOOD_MATCH, 14.041802, 0300743288131, 1 Er Tablets 1x100 Mfg. Abbott Laboratories 240 mg,1 count, Abbott Laboratories, JLI2V7, Healthcare, null, null, null]",
                Arrays.toString(result));

        result = transformer.transform(new MockInputRow().put(gtin, "300743288131"));
        assertEquals(
                "[GOOD_MATCH, 14.041802, 0300743288131, 1 Er Tablets 1x100 Mfg. Abbott Laboratories 240 mg,1 count, Abbott Laboratories, JLI2V7, Healthcare, null, null, null]",
                Arrays.toString(result));
        
        result = transformer.transform(new MockInputRow().put(gtin, "9999999999999"));
        assertEquals("[NO_MATCH, null, 9999999999999, null, null, null, null, null, null, null]", Arrays.toString(result));
        
        
        result = transformer.transform(new MockInputRow().put(gtin, "765390-68309"));
        assertEquals("[GOOD_MATCH, 14.041802, 0076539068309, Bbq Sauce, Naturally Fresh, KYSXQI, null, null, null, null]", Arrays.toString(result));
      
    }

    @Test
    public void testNormalizeGtinCode() throws Exception {
        assertEquals("0000000000002", ProductMatchTransformer.normalizeGtinCode("2"));
        assertEquals("0300743288131", ProductMatchTransformer.normalizeGtinCode("0300743288131"));
        assertEquals("0300743288131", ProductMatchTransformer.normalizeGtinCode("300743288131"));
        assertEquals("0000123423499", ProductMatchTransformer.normalizeGtinCode("12-34_234 9_9 "));
        assertEquals("0000123423499", ProductMatchTransformer.normalizeGtinCode("123423499"));
        assertEquals(null, ProductMatchTransformer.normalizeGtinCode(""));
        assertEquals(null, ProductMatchTransformer.normalizeGtinCode(null));
    }

    /**
     * Convenient factory for the transformer to use in this test class
     * 
     * @param columns
     * @return
     */
    private ProductMatchTransformer createTransformer(InputColumn<?>... columns) {
        final ProductMatchTransformer transformer = new ProductMatchTransformer();

        final ProductInputField[] inputFields = new ProductInputField[columns.length];
        for (int i = 0; i < inputFields.length; i++) {
            final ProductInputField inputField;
            final InputColumn<?> column = columns[i];
            if (column == product) {
                inputField = ProductInputField.PRODUCT_NAME;
            } else if (column == brand) {
                inputField = ProductInputField.BRAND_NAME;
            } else if (column == gtin) {
                inputField = ProductInputField.GTIN_CODE;
            } else {
                inputField = ProductInputField.PRODUCT_DESCRIPTION_TEXT;
            }
            inputFields[i] = inputField;
        }

        transformer.inputColumns = columns;
        transformer.inputMapping = inputFields;

        transformer.init();

        return transformer;
    }
}
