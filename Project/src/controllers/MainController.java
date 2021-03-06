package controllers;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import com.sun.javafx.scene.traversal.Direction;

import control.ArrowButton;
import control.ModalPopup;
import control.NavigationButton;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import se.chalmers.ait.dat215.project.IMatDataHandler;
import se.chalmers.ait.dat215.project.Order;
import se.chalmers.ait.dat215.project.ShoppingItem;
import util.BindingGroup;
import util.ContentView;
import util.InformationStorage;
import util.ShoppingCartHandler;
import util.ViewDisplay;

public class MainController implements Initializable {
	@FXML private StackPane contentPane;
	@FXML private ArrowButton prevButton;
	@FXML private ArrowButton nextButton;
	
	@FXML private Button contactButton, historyButton, helpButton;
	@FXML private Pane popupShadePane, removeShadePane;
	
	//Labels for describing the different steps
	@FXML private Label storeLabel, credentialsLabel, purchaseLabel, receiptLabel;
	

	private static MainController me;
	private static IMatDataHandler db = IMatDataHandler.getInstance();
	
	//Navigation buttons
	@FXML private NavigationButton btnToStore, btnToCredentials, btnToPurchase, btnToReceipt;
	
	private Pane dummyPane = new Pane();
	
	//The close-to-root stuff
	@FXML private Pane shoppingCart;
	@FXML private StackPane rootStackPane, popupPane, wrapperStackPane;
	@FXML private VBox mainContentWrapper;
	
	//The different views
	@FXML private Pane storePane, credentialsPane, purchasePane, receiptPane;
	
	//Popups
	@FXML private ModalPopup purchaseHistoryPopup, loadListPopup, saveListPopup, contactPopup, helpPopup;

	
	//Controllers
	@FXML private StoreController storePaneController;
	@FXML private CredentialsController credentialsPaneController;
	@FXML private PurchaseTestController purchasePaneController;
	@FXML private PurchaseHistoryController purchaseHistoryPopupController;
	@FXML private LoadListController loadListPopupController;
	@FXML private SaveListController saveListPopupController;
	@FXML private ShoppingCartController shoppingCartController;
	
	@FXML private ImageView fromStoreArrow, fromCredentialsArrow, fromPurchaseArrow;
	
	ViewDisplay viewDisplay;
	
	private ShoppingCartHandler cartHandler = ShoppingCartHandler.getInstance();
	
	public void initialize(URL url, ResourceBundle bundle) {
		prevButton.disable();
		nextButton.disable();
		
		me = this;
		
		popupShadePane.setMouseTransparent(true);
		
		//Necessary to disable the last arrow
		btnToReceipt.setUserData("lastButton");
		
		//Give the popup-system required panes
		ModalPopup.initialize(popupPane, wrapperStackPane, popupShadePane);
		
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
		
		//Define necessary bindings
		PURCHASE_VIEW_ACTIVE = activeViewBinding(purchaseView);
		RECEIPT_VIEW_ACTIVE = activeViewBinding(receiptView);
		
		//Setup bindings that determine how you can move between views
		//setStoreBinds();
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
		nextButton.setOnAction(event -> {
			//If we are on purchase, going to the next view means finishing the purchase.
			if (viewDisplay.getCurrentView().get().equals(purchaseView))
				sendOrder().handle(event);
			else
				viewDisplay.next();
		});
		
		//Initialize the navigation buttons
		btnToStore.initialize(storeView, show(storeView), btnToCredentials, storeLabel, null);
		btnToCredentials.initialize(credentialsView, show(credentialsView), btnToPurchase, credentialsLabel, fromStoreArrow);
		btnToPurchase.initialize(purchaseView, show(purchaseView), btnToReceipt, purchaseLabel, fromCredentialsArrow);
		btnToReceipt.initialize(receiptView, sendOrder(), null, receiptLabel, fromPurchaseArrow);
		
		shoppingCartController.getShoppingListButton().setOnAction(e -> loadListPopup.show());
		shoppingCartController.getSaveListButton().setOnAction(e -> saveListPopup.show());
		
		contactButton.setOnAction(e -> contactPopup.show());
		historyButton.setOnAction(e -> { purchaseHistoryPopupController.update(); purchaseHistoryPopup.show();});
		helpButton.setOnAction(e -> helpPopup.show());

		//Drag & drop:  resizing of window to 'highlight' the shopping cart.
		purchaseHistoryPopup.setOnDragOver(e -> {
			popupPane.setMouseTransparent(true);
			removeShadePane.getStyleClass().add("transparent");
		});
		
		//Show the store
		viewDisplay.show(storeView);
		
		wrapperStackPane.toFront();
	}
	
	private final BooleanBinding CART_NONEMPTY = Bindings.createBooleanBinding(() -> !cartHandler.emptyProperty().get(), cartHandler.emptyProperty());
	private BooleanBinding PURCHASE_VIEW_ACTIVE;
	private BooleanBinding RECEIPT_VIEW_ACTIVE;
	
	/** Creates a binding that is true when the active view is the view with "css-id" <code>ID</code> */
	private BooleanBinding activeViewBinding(ContentView view) {
		SimpleBooleanProperty isActiveView = new SimpleBooleanProperty(false);
		viewDisplay.getCurrentView().addListener((obs, oldValue, newValue) ->  isActiveView.set(viewDisplay.getCurrentView().get().equals(view)));
		BooleanBinding binding = Bindings.createBooleanBinding(() -> isActiveView.get(), isActiveView);
		
		return binding;
	}
	
	private void setCredentialBinds() {
		ContentView view = viewDisplay.getView(credentialsPane);
		
		BindingGroup group = btnToCredentials.getBindingGroup();
		group.addBinding(CART_NONEMPTY.and(not(RECEIPT_VIEW_ACTIVE)));
		group.getState().addListener((obs, o, n) -> {
			ContentView storeView = viewDisplay.getView(storePane);
			ContentView receiptView = viewDisplay.getView(storePane);
			ContentView currentView = viewDisplay.getCurrentView().getValue();
			
			if (not(CART_NONEMPTY).get() && !currentView.equals(storeView) && !currentView.equals(receiptView))
				viewDisplay.show(storeView);
		
		btnToCredentials.setDisable(true);
		});
		
		view.getBindingGroup().setAll(group.getBinds());
	}
	
	/** Event that shows the view <code>view</code> */
	private EventHandler<ActionEvent> show(ContentView view) {
		return e -> viewDisplay.show(view);
	}
	
	private void setPurchaseBinds() {
		ContentView view = viewDisplay.getView(purchasePane);
		BindingGroup group = btnToPurchase.getBindingGroup();
		group.addBindings(credentialsPaneController.getBindings().and(CART_NONEMPTY).and(not(RECEIPT_VIEW_ACTIVE)));
		
		view.getBindingGroup().setAll(group.getBinds());
	}

	private void setReceiptBinds() {
		ContentView view = viewDisplay.getView(receiptPane);
		BindingGroup group = btnToReceipt.getBindingGroup();
		group.addBinding(purchasePaneController.getBindings().and(PURCHASE_VIEW_ACTIVE));
		
		view.getBindingGroup().setAll(group.getBinds());
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
	
	public ArrowButton getArrowButton(Direction direction) {
		if (direction == Direction.LEFT)
			return prevButton;
		else
			return nextButton;
	}
	
	/** Is called when a purchase has completed. */
	private EventHandler<ActionEvent> sendOrder() {
		return e -> {
			viewDisplay.show(viewDisplay.getView(receiptPane));
			
			ShoppingCartHandler handler = ShoppingCartHandler.getInstance();
			List<ShoppingItem> items = handler.getCartItems();
			
			items.forEach(item -> db.getShoppingCart().addItem(item));


			db.placeOrder();
			handler.clearCart();
			db.shutDown();
			
			//---------------//
			List<Order> orders = IMatDataHandler.getInstance().getOrders();
			
			Collections.sort(orders, (o1, o2) -> {
				return (int) (o2.getDate().getTime() - o1.getDate().getTime());
			});
			
			float totalPrice = 0;
			for (ShoppingItem item : orders.get(0).getItems()){
				totalPrice += item.getTotal();
			}

			RecipeController.getInstance().setTitleText(InformationStorage.getFirstName());
			RecipeController.getInstance().setPriceText(totalPrice);
			RecipeController.getInstance().setDeliveryTimeText(InformationStorage.getDelivery());
			RecipeController.getInstance().setPaymentText(InformationStorage.getPaymentType());
		};
	}
	
	public BooleanBinding not(BooleanBinding binding) {
		return binding.not();
	}
	
	public ModalPopup getHistoryPopup() {
		return this.purchaseHistoryPopup;
	}
	
	public void restoreShade() {
		removeShadePane.getStyleClass().remove("transparent");
		popupPane.setMouseTransparent(false);
	}
}