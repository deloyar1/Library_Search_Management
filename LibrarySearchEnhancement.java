package com.example.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class LibrarySearchEnhancement extends Application {

    static class Book {
        private final String title;
        private final String author;
        public Book(String title, String author) {
            this.title = title;
            this.author = author;
        }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }


        public String toString() {
            return title;
        }
    }

    static class FileManager {
        public static List<Book> loadBooks(String filePath) {
            List<Book> books = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        books.add(new Book(parts[0].trim(), parts[1].trim()));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return books;
        }

        public static void saveBook(String filePath, Book book) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                writer.write(book.getTitle() + "," + book.getAuthor());
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void deleteBook(String filePath, Book bookToDelete) {
            List<Book> books = loadBooks(filePath);
            books = books.stream()
                    .filter(book -> !(book.getTitle().equals(bookToDelete.getTitle()) &&
                            book.getAuthor().equals(bookToDelete.getAuthor())))
                    .collect(Collectors.toList());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                for (Book b : books) {
                    writer.write(b.getTitle() + "," + b.getAuthor());
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class SearchEngine {
        public static int levenshtein(String a, String b) {
            int[][] dp = new int[a.length() + 1][b.length() + 1];
            for (int i = 0; i <= a.length(); i++) {
                for (int j = 0; j <= b.length(); j++) {
                    if (i == 0) dp[i][j] = j;
                    else if (j == 0) dp[i][j] = i;
                    else if (a.charAt(i - 1) == b.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1];
                    else dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
            return dp[a.length()][b.length()];
        }

        public static Map<Book, Integer> search(List<Book> books, String query) {
            Map<Book, Integer> matched = new LinkedHashMap<>();
            for (Book book : books) {
                String title = book.getTitle();
                int dist = levenshtein(query.toLowerCase(), title.toLowerCase());
                int maxLen = Math.max(query.length(), title.length());
                int similarity = 100 - (dist * 100 / maxLen);
                if (similarity >= 40) {
                    matched.put(book, similarity);
                }
            }
            return matched;
        }
    }

    private static final String FILE_PATH = "books.txt";


    public void start(Stage primaryStage) {
        TextField searchField = new TextField();
        searchField.setPromptText("Enter book title");

        Button searchBtn = new Button("Search");
        Button deleteBtn = new Button("Delete Selected");

        ListView<String> resultsView = new ListView<>();
        ObservableList<String> resultList = FXCollections.observableArrayList();
        resultsView.setItems(resultList);

        Map<String, Book> displayToBookMap = new HashMap<>();

        searchBtn.setOnAction(e -> {
            String query = searchField.getText().trim();
            resultList.clear();
            displayToBookMap.clear();

            if (!query.isEmpty()) {
                List<Book> books = FileManager.loadBooks(FILE_PATH);
                Map<Book, Integer> results = SearchEngine.search(books, query);

                if (results.isEmpty()) {
                    resultList.add("No close matches found.");
                } else {
                    for (Book b : results.keySet()) {
                        String display = b.getTitle() + " by " + b.getAuthor(); // title + author
                        resultList.add(display);
                        displayToBookMap.put(display, b);
                    }

                }
            }
        });

        deleteBtn.setOnAction(e -> {
            String selected = resultsView.getSelectionModel().getSelectedItem();
            if (selected != null && displayToBookMap.containsKey(selected)) {
                Book toDelete = displayToBookMap.get(selected);
                FileManager.deleteBook(FILE_PATH, toDelete);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Deleted: " + toDelete.getTitle());
                alert.show();
                searchBtn.fire();
            }
        });


        TextField titleField = new TextField();
        titleField.setPromptText("Book Title");

        TextField authorField = new TextField();
        authorField.setPromptText("Author");

        Button addBtn = new Button("Add Book");

        addBtn.setOnAction(e -> {
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            if (!title.isEmpty() && !author.isEmpty()) {
                Book newBook = new Book(title, author);
                FileManager.saveBook(FILE_PATH, newBook);
                titleField.clear();
                authorField.clear();
                if (!searchField.getText().isEmpty()) {
                    searchBtn.fire();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please enter both title and author.");
                alert.show();
            }
        });

        VBox searchBox = new VBox(10, searchField, searchBtn, resultsView, deleteBtn);
        searchBox.setPrefWidth(500);
        searchBox.setPadding(new javafx.geometry.Insets(10));

        VBox addBox = new VBox(10, new Label("Add New Book"), titleField, authorField, addBtn);
        addBox.setPadding(new javafx.geometry.Insets(10));
        addBox.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5;");

        HBox root = new HBox(20, searchBox, addBox);
        root.setPadding(new javafx.geometry.Insets(15));

        Scene scene = new Scene(root, 800, 400);
        primaryStage.setTitle("Library Search Enhancement");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
