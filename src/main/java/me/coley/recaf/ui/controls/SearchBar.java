package me.coley.recaf.ui.controls;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import me.coley.recaf.util.struct.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * Basic search bar.
 *
 * @author Matt
 */
public class SearchBar extends GridPane {
	// Actions
	private Runnable onCloseIntent;
	private Consumer<Results> onSearch;
	// UI
	private final Label lblResults = new Label();
	private final TextField txtSearch = new TextField();
	Button clearSearch = new ActionButton(null, this::resetSearch);
	Button closeSearch = new ActionButton(null, this::closeSearch);
	Button toggleCase = new ActionButton(null, this::toggleCase);
	Button toggleRegex = new ActionButton(null, this::toggleRegex);
	private final Supplier<String> text;
	// inputs
	private boolean dirty = true;
	private String lastSearchText;
	// last result
	private Results results;
	private boolean matchCase = false;
	private boolean regex = false;

	/**
	 * @param text
	 * 		Supplier of searchable text.
	 */
	public SearchBar(Supplier<String> text) {
		setAlignment(Pos.CENTER_LEFT);
		setHgap(7);
		ColumnConstraints columnInput = new ColumnConstraints();
		columnInput.setPercentWidth(30);
		ColumnConstraints columnOptions = new ColumnConstraints();
		columnOptions.setPercentWidth(70);
		getColumnConstraints().addAll(columnInput, columnOptions);
		clearSearch.setGraphic(new IconView("icons/close.png"));
		clearSearch.setTooltip(new Tooltip("Clear search"));
		toggleCase.setText("Aa");
		toggleCase.setTooltip(new Tooltip("Match case"));
		toggleRegex.setText(".*");
		toggleRegex.setTooltip(new Tooltip("Regex"));
		closeSearch.setGraphic(new IconView("icons/close.png"));
		closeSearch.setTooltip(new Tooltip("Close search"));
		this.text = text;
		getStyleClass().add("context-menu");
		txtSearch.getStyleClass().add("search-field");
		txtSearch.setOnKeyPressed(this::handleKeypress);
		HBox hBox = new HBox(0, clearSearch, toggleCase, toggleRegex, lblResults, closeSearch);
		HBox.setHgrow(lblResults, Priority.ALWAYS);
		hBox.setAlignment(Pos.CENTER);
		hBox.setSpacing(5);
		lblResults.setMaxWidth(Double.MAX_VALUE);
		add(txtSearch, 0, 0);
		add(hBox, 1, 0);
	}

	/**
	 * @param onSearch
	 * 		Search result handler to run.
	 */
	public void setOnSearch(Consumer<Results> onSearch) {
		this.onSearch = onSearch;
	}
	/**
	 * @param onCloseIntent
	 * 		Escape handler to run.
	 */
	public void setOnCloseIntent(Runnable onCloseIntent) {
		this.onCloseIntent = onCloseIntent;
	}

	/**
	 * Focus the search bar input text-field.
	 */
	public void focus() {
		txtSearch.requestFocus();
		txtSearch.selectAll();
	}

	/**
	 * Clear the search bar display.
	 */
	public void clear() {
		txtSearch.clear();
		lblResults.setText("");
	}

	/**
	 * Reset the search.
	 */
	public void resetSearch() {
		clear();
		txtSearch.requestFocus();
	}

	private void handleKeypress(KeyEvent e) {
		// Check if we've updated the search query
		String searchText = matchCase ? txtSearch.getText() : txtSearch.getText().toLowerCase();
		if(!searchText.equals(lastSearchText)) {
			dirty = true;
		}
		lastSearchText = searchText;
		// Handle operations
		if(e.getCode() == KeyCode.ESCAPE) {
			// Escape the search bar
			closeSearch();
		} else {
			// Empty check
			if (searchText.isEmpty()) {
				results = null;
				return;
			}
			// Find next
			//  - Run search if necessary
			if(dirty) {
				results = search();
				dirty = false;
			}
			if(onSearch != null && results != null)
				onSearch.accept(results);
		}
	}
	/**
	 * Toggle case sensitivity
	 */
	private void toggleCase() {
		matchCase = !matchCase;
		String searchText = matchCase ? txtSearch.getText() : txtSearch.getText().toLowerCase();
		if (!searchText.isEmpty() && !lastSearchText.equals(searchText)) {
			lastSearchText = searchText;
			results = search();
			if (onSearch != null)
				onSearch.accept(results);
		}
		txtSearch.requestFocus();
	}

	/**
	 * Toggle regex in search field
	 */
	private void toggleRegex() {
		regex = !regex;
		txtSearch.requestFocus();
	}

	private void closeSearch() {
		if(onCloseIntent != null)
			onCloseIntent.run();
	}

	/**
	 * @param text
	 * 		Text to set.
	 */
	public void setText(String text) {
		txtSearch.setText(text);
	}

	/**
	 * @return Search result ranges of the current search parameters.
	 */
	private Results search() {
		Results results = new Results();
		String searchText = txtSearch.getText();
		String targetText = text.get();
		if (!matchCase) {
			searchText = searchText.toLowerCase();
			targetText = targetText.toLowerCase();
		}
		int len = searchText.length();
		int index = targetText.indexOf(searchText);
		while(index >= 0) {
			// Add result
			results.add(index, index + len);
			// Find next
			index = targetText.indexOf(searchText, index + len);
		}
		return results;
	}

	/**
	 * Search results wrapper.
	 *
	 * @author Matt
	 */
	public class Results {
		private final List<Pair<Integer, Integer>> ranges = new ArrayList<>();

		private void add(int start, int end) {
			ranges.add(new Pair<>(start, end));
		}

		/**
		 * @param caret
		 * 		Caret position in text.
		 *
		 * @return Next range immediately after the caret position.
		 */
		public Pair<Integer, Integer> next(int caret) {
			// Check for no matches
			if(ranges.isEmpty()) {
				lblResults.setText(translate("ui.search.results.none"));
				return null;
			}
			// Find first result where the caret is before the result range
			Pair<Integer, Integer> match = null;
			int i = 1;
			for(Pair<Integer, Integer> range : ranges) {
				if(caret < range.getKey()) {
					match = range;
					break;
				}
				i++;
			}
			// No match after caret position, wrap around
			if(match == null) {
				i = 1;
				match = ranges.get(0);
			}
			lblResults.setText(translate("ui.search.results.indexpre") + i + "/" + ranges.size());
			return match;
		}
	}
}
