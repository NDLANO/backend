/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class LanguageFieldTest {

    @Test
    void test_create_language_field() {
        var languageField = new LanguageField<String>();
        assertEquals(0, languageField.size());
    }

    @Test
    void test_create_language_field_from_node() {
        Node node = new Node(NodeType.NODE);
        node.setName("Name");
        node.addTranslation("Name", "nb");

        var languageField = LanguageField.fromNode(node);
        assertEquals(1, languageField.size());
        assertEquals("Name", languageField.get("nb"));
    }

    @Test
    void test_create_language_field_list_from_node() {
        Node node = new Node(NodeType.NODE);
        node.setName("Name");
        node.addTranslation("Name", "nb");

        var languageField = LanguageField.listFromNode(node);
        assertEquals(1, languageField.size());
        assertEquals(List.of("Name"), languageField.get("nb"));
    }

    @Test
    void test_create_language_field_list_from_old_list_and_node() {
        Node node = new Node(NodeType.NODE);
        node.setName("Name");
        node.addTranslation("Name", "nb");

        Node node2 = new Node(NodeType.NODE);
        node2.setName("Name 2");
        node2.addTranslation("Name 2", "nb");

        var languageField =
                LanguageField.listFromLists(LanguageField.listFromNode(node), LanguageField.fromNode(node2));
        assertEquals(1, languageField.size());
        assertEquals(List.of("Name", "Name 2"), languageField.get("nb"));
    }

    @Test
    void test_create_language_field_list_without_changing_original_list() {
        Node node = new Node(NodeType.NODE);
        node.setName("Name");
        node.addTranslation("Name", "nb");

        Node node2 = new Node(NodeType.NODE);
        node2.setName("Name 2");
        node2.addTranslation("Name 2", "nb");

        Node node3 = new Node(NodeType.NODE);
        node3.setName("Name 3");
        node3.addTranslation("Name 3", "nb");

        var languageField =
                LanguageField.listFromLists(LanguageField.listFromNode(node), LanguageField.fromNode(node2));
        assertEquals(1, languageField.size());
        assertEquals(List.of("Name", "Name 2"), languageField.get("nb"));

        var updatedLanguageField = LanguageField.listFromLists(languageField, LanguageField.fromNode(node3));
        assertEquals(1, updatedLanguageField.size());
        assertEquals(List.of("Name", "Name 2", "Name 3"), updatedLanguageField.get("nb"));
        // original list not updated
        assertEquals(List.of("Name", "Name 2"), languageField.get("nb"));
    }

    @Test
    void test_create_language_field_list_with_complete_crumbs() {
        Node node = new Node(NodeType.NODE);
        node.setName("Name");
        node.addTranslation("Name nb", "nb");
        node.addTranslation("Name nn", "nn");

        Node node2 = new Node(NodeType.NODE);
        node2.setName("Name 2");
        node2.addTranslation("Name 2 nb", "nb");
        node2.addTranslation("Name 2 en", "en");

        var languageField =
                LanguageField.listFromLists(LanguageField.listFromNode(node), LanguageField.fromNode(node2));
        assertEquals(3, languageField.size());
        assertEquals(List.of("Name nb", "Name 2 nb"), languageField.get("nb"));
        assertEquals(List.of("Name nn", "Name 2 nb"), languageField.get("nn"));
        assertEquals(List.of("Name nb", "Name 2 en"), languageField.get("en"));

        Node node3 = new Node(NodeType.NODE);
        node3.setName("Name 3");
        node3.addTranslation("Name 3 se", "se");

        var languageField2 = LanguageField.listFromLists(languageField, LanguageField.fromNode(node3));
        assertEquals(4, languageField2.size());
        assertEquals(List.of("Name nb", "Name 2 nb", "Name 3"), languageField2.get("nb"));
        assertEquals(List.of("Name nn", "Name 2 nb", "Name 3"), languageField2.get("nn"));
        assertEquals(List.of("Name nb", "Name 2 en", "Name 3"), languageField2.get("en"));
        assertEquals(List.of("Name nb", "Name 2 nb", "Name 3 se"), languageField2.get("se"));
    }
}
