package org.smm.archetype.component.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.component.dto.SearchQuery;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 client-search 模块遵循 Template Method 模式。
 * AbstractSearchComponent 应为抽象类，公开方法应为 final。
 */
@DisplayName("AbstractSearchComponent Template Method 模式")
class AbstractSearchComponentUTest {

    @Nested
    @DisplayName("类存在性验证")
    class ClassExistence {

        @Test
        @DisplayName("AbstractSearchComponent 应存在且为抽象类")
        void should_exist_and_be_abstract() {
            Class<?> clazz = AbstractSearchComponent.class;
            assertThat(clazz).isNotNull();
            assertThat(Modifier.isAbstract(clazz.getModifiers())).isTrue();
            assertThat(SearchComponent.class).isAssignableFrom(clazz);
        }

        @Test
        @DisplayName("SimpleSearchComponent 应继承 AbstractSearchComponent")
        void simpleSearchComponent_should_extend_AbstractSearchComponent() {
            Class<?> superclass = SimpleSearchComponent.class.getSuperclass();
            assertThat(superclass).isEqualTo(AbstractSearchComponent.class);
        }

        @Test
        @DisplayName("SearchComponent 接口应包含 15 个方法")
        void searchClient_should_have_15_methods() {
            Method[] methods = SearchComponent.class.getDeclaredMethods();
            assertThat(methods.length)
                    .as("SearchComponent 接口应包含 15 个方法")
                    .isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("final 方法验证")
    class FinalMethods {

        @Test
        @DisplayName("search(SearchQuery) 方法应为 final")
        void search_should_be_final() throws NoSuchMethodException {
            Method searchMethod = AbstractSearchComponent.class.getMethod("search", SearchQuery.class);
            assertThat(Modifier.isFinal(searchMethod.getModifiers()))
                    .as("search() 方法应为 final")
                    .isTrue();
        }

        @Test
        @DisplayName("index 方法应为 final")
        void index_should_be_final() throws NoSuchMethodException {
            Method indexMethod = AbstractSearchComponent.class.getMethod("index", String.class, String.class, Object.class);
            assertThat(Modifier.isFinal(indexMethod.getModifiers()))
                    .as("index() 方法应为 final")
                    .isTrue();
        }

        @Test
        @DisplayName("delete 方法应为 final")
        void delete_should_be_final() throws NoSuchMethodException {
            Method deleteMethod = AbstractSearchComponent.class.getMethod("delete", String.class, String.class);
            assertThat(Modifier.isFinal(deleteMethod.getModifiers()))
                    .as("delete() 方法应为 final")
                    .isTrue();
        }

        @Test
        @DisplayName("get 方法应为 final")
        void get_should_be_final() throws NoSuchMethodException {
            Method getMethod = AbstractSearchComponent.class.getMethod("get", String.class, String.class);
            assertThat(Modifier.isFinal(getMethod.getModifiers()))
                    .as("get() 方法应为 final")
                    .isTrue();
        }

        @Test
        @DisplayName("bulkIndex 方法应为 final")
        void bulkIndex_should_be_final() throws NoSuchMethodException {
            Method bulkIndexMethod = AbstractSearchComponent.class.getMethod("bulkIndex", String.class, List.class);
            assertThat(Modifier.isFinal(bulkIndexMethod.getModifiers()))
                    .as("bulkIndex() 方法应为 final")
                    .isTrue();
        }

        @Test
        @DisplayName("createIndex 方法应为 final")
        void createIndex_should_be_final() throws NoSuchMethodException {
            Method createMethod = AbstractSearchComponent.class.getMethod("createIndex", String.class);
            assertThat(Modifier.isFinal(createMethod.getModifiers()))
                    .as("createIndex() 方法应为 final")
                    .isTrue();
        }

        @Test
        @DisplayName("deleteIndex(String) 方法应为 final")
        void deleteIndex_should_be_final() throws NoSuchMethodException {
            Method deleteIndexMethod = AbstractSearchComponent.class.getMethod("deleteIndex", String.class);
            assertThat(Modifier.isFinal(deleteIndexMethod.getModifiers()))
                    .as("deleteIndex() 方法应为 final")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("do* 扩展点验证")
    class DoExtensionPoints {

        @Test
        @DisplayName("doSearch 应为 protected 抽象方法")
        void doSearch_should_be_protected_abstract() throws NoSuchMethodException {
            Method doSearchMethod = AbstractSearchComponent.class.getDeclaredMethod("doSearch", SearchQuery.class);
            assertThat(Modifier.isProtected(doSearchMethod.getModifiers()))
                    .as("doSearch() 应为 protected")
                    .isTrue();
            assertThat(Modifier.isAbstract(doSearchMethod.getModifiers()))
                    .as("doSearch() 应为 abstract")
                    .isTrue();
        }

        @Test
        @DisplayName("doIndex 应为 protected 抽象方法")
        void doIndex_should_be_protected_abstract() throws NoSuchMethodException {
            Method doIndexMethod = AbstractSearchComponent.class.getDeclaredMethod("doIndex", String.class, String.class, Object.class);
            assertThat(Modifier.isProtected(doIndexMethod.getModifiers())).isTrue();
            assertThat(Modifier.isAbstract(doIndexMethod.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("doDelete 应为 protected 抽象方法")
        void doDelete_should_be_protected_abstract() throws NoSuchMethodException {
            Method doDeleteMethod = AbstractSearchComponent.class.getDeclaredMethod("doDelete", String.class, String.class);
            assertThat(Modifier.isProtected(doDeleteMethod.getModifiers())).isTrue();
            assertThat(Modifier.isAbstract(doDeleteMethod.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("doGet 应为 protected 抽象方法")
        void doGet_should_be_protected_abstract() throws NoSuchMethodException {
            Method doGetMethod = AbstractSearchComponent.class.getDeclaredMethod("doGet", String.class, String.class);
            assertThat(Modifier.isProtected(doGetMethod.getModifiers())).isTrue();
            assertThat(Modifier.isAbstract(doGetMethod.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("doBulkIndex 应为 protected 抽象方法")
        void doBulkIndex_should_be_protected_abstract() throws NoSuchMethodException {
            Method doBulkIndexMethod = AbstractSearchComponent.class.getDeclaredMethod("doBulkIndex", String.class, List.class);
            assertThat(Modifier.isProtected(doBulkIndexMethod.getModifiers())).isTrue();
            assertThat(Modifier.isAbstract(doBulkIndexMethod.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("doDeleteIndex 应为 protected 抽象方法")
        void doDeleteIndex_should_be_protected_abstract() throws NoSuchMethodException {
            Method doDeleteIndexMethod = AbstractSearchComponent.class.getDeclaredMethod("doDeleteIndex", String.class);
            assertThat(Modifier.isProtected(doDeleteIndexMethod.getModifiers())).isTrue();
            assertThat(Modifier.isAbstract(doDeleteIndexMethod.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("所有 do* 方法应恰好有 14 个 protected abstract 扩展点")
        void do_methods_should_have_14_extension_points() {
            Set<String> doMethodNames = Arrays.stream(AbstractSearchComponent.class.getDeclaredMethods())
                    .filter(m -> m.getName().startsWith("do"))
                    .filter(m -> Modifier.isProtected(m.getModifiers()))
                    .filter(m -> Modifier.isAbstract(m.getModifiers()))
                    .map(Method::getName)
                    .collect(Collectors.toSet());

            assertThat(doMethodNames)
                    .as("应有 14 个 protected abstract do* 扩展点")
                    .hasSize(14);
        }
    }
}
