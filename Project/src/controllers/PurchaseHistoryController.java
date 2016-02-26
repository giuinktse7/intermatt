package controllers;

import java.net.URL;
import java.util.ResourceBundle;

import interfaces.Action;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import se.chalmers.ait.dat215.project.IMatDataHandler;
import util.SimpleTable;

public class PurchaseHistoryController implements Initializable {
	
	@FXML private Button closeButton;
	@FXML private Pane bottomPane;
	@FXML private Pane contentPane;
	@FXML private VBox tableBox;
	
	
	private IMatDataHandler db = IMatDataHandler.getInstance();
	
	public void initialize(URL location, ResourceBundle resources) {
		
		DropShadow dropShadow = new DropShadow();
		 dropShadow.setRadius(5.0);
		 dropShadow.setOffsetX(3.0);
		 dropShadow.setOffsetY(3.0);
		 dropShadow.setColor(Color.color(0.4, 0.5, 0.5));
		 contentPane.setEffect(dropShadow);
		
		
		SimpleTable table = new SimpleTable(tableBox, "Date", "Order ID");
		db.getShoppingCart().addProduct(db.getProduct(0));
		table.addOrder(db.placeOrder());
	}
	
	public void setCloseAction(Action c) {
		closeButton.setOnAction(e -> c.call());
		bottomPane.setOnMouseClicked(e -> c.call());
	}
	
}