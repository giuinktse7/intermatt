package controllers;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.sun.javafx.scene.traversal.Direction;

import control.ArrowButton;
import control.ModalPopup;
import control.NavigationButton;
import interfaces.Action;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import se.chalmers.ait.dat215.project.IMatDataHandler;
import se.chalmers.ait.dat215.project.ShoppingItem;
import util.BindingGroup;
import util.ContentView;
import util.ShoppingCartHandler;
import util.ViewDisplay;

public class MainController implements Initializable {
	@FXML private StackPane contentPane;
	@FXML private ArrowButton prevButton;
	@FXML private ArrowButton nextButton;
	@FXML private Button purchaseHistoryButton;
	
	//Used for the drag & drop functionality
	private final int AVG = 1374;

	private static MainController me;
	private static IMatDataHandler db = IMatDataHandler.getInstance();
	
	//Navigation buttons
	@FXML private NavigationButton btnToStore;
	@FXML private NavigationButton btnToCredentials;
	@FXML private NavigationButton btnToPurchase;
	@FXML private NavigationButton btnToReceipt;
	
	private Pane dummyPane = new Pane();
	
	//The close-to-root stuff
	@FXML private Pane shoppingCart;
	@FXML private SplitPane splitPane;
	@FXML private StackPane wrapperStackPane;
	@FXML private VBox mainContentWrapper;
	
	//The different views
	@FXML private Pane storePane;
	@FXML private Pane credentialsPane;
	@FXML private Pane purchasePane;
	@FXML private Pane receiptPane;
	
	//Popups
	@FXML private ModalPopup purchaseHistoryPopup;
	@FXML private ModalPopup loadListPopup;
	@FXML private ModalPopup saveListPopup;
	
	//Controllers
	@FXML private StoreController storePaneController;
	@FXML private CredentialsController credentialsPaneController;
	@FXML private PurchaseController purchasePaneController;
	@FXML private PurchaseHistoryController purchaseHistoryPopupController;
	@FXML private LoadListController loadListPopupController;
	@FXML private SaveListController saveListPopupController;
	@FXML private ShoppingCartController shoppingCartController;
	
	ViewDisplay viewDisplay;
	
	private ShoppingCartHandler cartHandler = ShoppingCartHandler.getInstance();
	
	public void initialize(URL url, ResourceBundle bundle) {
		prevButton.disable();
		nextButton.disable();
		
		
		me = this;
		
		//Give the popup-system required panes
		ModalPopup.initialize(wrapperStackPane, mainContentWrapper);
		
		//Start viewDisplay-system
		viewDisplay = new ViewDisplay(contentPane);
		NavigationButton.setViewDisplay(viewDisplay);
		
		//Create the views,
		ContentView storeView = new ContentView(storePane);
		ContentView credentialsView = new ContentView(credentialsPane);
		ContentView purchaseView = new ContentView(purchasePane);
		ContentView receiptView = new ContentView(receiptPane);
		ContentView dummyView = new ContentView(dummyPane);
		
		//and add the views to the viewDisplay
		viewDisplay.addView(storeView);
		viewDisplay.addView(credentialsView);
		viewDisplay.addView(purchaseView);
		viewDisplay.addView(receiptView);
		viewDisplay.addView(dummyView);
		
		//Show the store
		viewDisplay.show(storeView);
		
		//Define necessary bindings
		PURCHASE_VIEW_ACTIVE = activeViewBinding("purchasePane");
		RECEIPT_VIEW_ACTIVE = activeViewBinding("purchasePane");
		
		//Setup bindings that determine how you can move between views
		setCredentialBinds();
		setPurchaseBinds();
		setReceiptBinds();
		
		//Set next & previous relationships
		storeView.setNext(credentialsView);
		credentialsView.setNext(purchaseView);
		credentialsView.setPrevious(storeView);
		purchaseView.setPrevious(credentialsView);
		purchaseView.setNext(receiptView);
		receiptView.setNext(storeView);
		receiptView.setPrevious(null);
		
		//Set actions for the previous & next buttons
		prevButton.setDirection(Direction.LEFT);
		nextButton.setDirection(Direction.RIGHT);
		prevButton.setOnAction(event -> viewDisplay.previous());
		nextButton.setOnAction(event -> viewDisplay.next());
		
		//Initialize the navigation buttons
		btnToStore.initialize(storeView,show(storeView),btnToCredentials);
		btnToCredentials.initialize(credentialsView, show(credentialsView), btnToPurchase);
		btnToPurchase.initialize(purchaseView, show(purchaseView), btnToReceipt);
		btnToReceipt.initialize(receiptView, show(receiptView), null);
		
		//Setup the different popup buttons
		purchaseHistoryButton.setOnAction(event -> { purchaseHistoryPopupController.update(); purchaseHistoryPopup.show(); });
		shoppingCartController.getShoppingListButton().setOnAction(e -> loadListPopup.show());
		shoppingCartController.getSaveListButton().setOnAction(e -> saveListPopup.show());

		//Drag & drop:  resizing of window to 'highlight' the shopping cart.
		purchaseHistoryPopup.setOnDragOver(e -> {
			purchaseHistoryPopup.setMaxWidth(1000 + (nextButton.getScene().getWidth() - AVG) / 2);
			purchaseHistoryPopup.setPrefWidth(1000 + (nextButton.getScene().getWidth() - AVG) / 2);
			purchaseHistoryPopup.getContent().setAlignment(Pos.CENTER_RIGHT);
	});
	}
	
	private final BooleanBinding CART_NONEMPTY = Bindings.createBooleanBinding(() -> !cartHandler.emptyProperty().get(), cartHandler.emptyProperty());
	private BooleanBinding PURCHASE_VIEW_ACTIVE;
	private BooleanBinding RECEIPT_VIEW_ACTIVE;
	
	/** Creates a binding that is true when the active view is the view with "css-id" <code>ID</code> */
	private BooleanBinding activeViewBinding(String ID) {
		SimpleBooleanProperty isActiveView = new SimpleBooleanProperty(false);
		viewDisplay.getCurrentView().addListener((obs, oldValue, newValue) ->  isActiveView.set(viewDisplay.getCurrentView().getValue().getID().equals("purchasePane")));
		BooleanBinding binding = Bindings.createBooleanBinding(() -> isActiveView.get(), isActiveView);
		
		return binding;
	}
	
	private void setCredentialBinds() {
		ContentView view = viewDisplay.getView(credentialsPane);
		
		BindingGroup group = btnToCredentials.getBindingGroup();
		group.addBinding(CART_NONEMPTY);
		group.setOnFalseAction(() -> {
			ContentView store = viewDisplay.getView(storePane);
			if (!viewDisplay.getCurrentView().getValue().equals(store) && !viewDisplay.getCurrentView().getValue().getID().equals("receiptPane"))
				viewDisplay.show(store);
			
			btnToCredentials.setDisable(true);
		});
		view.getBindingGroup().setName("credentialsViewGroup");
		view.getBindingGroup().setAll(group.getBinds());
		
		hej
	}
	
	/** Event that shows the view <code>view</code> */
	private EventHandler<ActionEvent> show(ContentView view) {
		return e -> viewDisplay.show(view);
	}
	
	private void setPurchaseBinds() {
		ContentView view = viewDisplay.getView(purchasePane);
		view.getBindingGroup().setName("purchaseViewGroup");
		BindingGroup group = btnToPurchase.getBindingGroup();
		group.addBindings(credentialsPaneController.getBindings());
		
		view.getBindingGroup().setAll(group.getBinds());
	}
	
	private void setReceiptBinds() {
		ContentView view = viewDisplay.getView(receiptPane);
		BindingGroup group = btnToReceipt.getBindingGroup();
		group.addBinding(PURCHASE_VIEW_ACTIVE.or(RECEIPT_VIEW_ACTIVE));
		
		view.getBindingGroup().setAll(group.getBinds());
	}
	
	public void finishPurchase() {
		ShoppingCartHandler handler = ShoppingCartHandler.getInstance();
		List<ShoppingItem> items = handler.getCartItems();
		
		items.forEach(item -> db.getShoppingCart().addItem(item));


		db.placeOrder();
		handler.clearCart();
		db.shutDown();
	}

	public void restoreUserData(){
		credentialsPaneController.restore_user_data();
	}

	public void saveUserData(){
		credentialsPaneController.save_user_data();
	}
	
	public static MainController get() {
		return me;
	}
	
	private boolean isCurrentView(Pane pane) {
		return viewDisplay.getCurrentView().getValue().equals(viewDisplay.getView(pane));
	}
	
	/** Disables an ArrowButton if <code>pane</code> is on the currently selected view. */
	public Action disableButton(ArrowButton button, Pane pane) {
		return () -> {
			
			if (isCurrentView(pane))
				button.disable(); 
			};
	}

	/** Enables an ArrowButton if <code>pane</code> is on the currently selected view. */
	public Action enableButton(ArrowButton button, Pane pane) {
		return () -> {
			if (isCurrentView(pane))
				button.enable(); 
			};
	}
	
	// TODO Fix when home, use by the button in the last view
	public void setViewDisplay(String ID) {
	//	viewDisplay.show(viewDisplay.get);
	}
	
	public ArrowButton getArrowButton(Direction direction) {
		if (direction == Direction.LEFT)
			return prevButton;
		else
			return nextButton;
	}
}