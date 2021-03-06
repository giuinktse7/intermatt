package control;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import se.chalmers.ait.dat215.project.Order;
import se.chalmers.ait.dat215.project.ShoppingItem;

/** Box that is used for the overview-view of Orders in the "purchase history" view. */
public class OrderOverviewBox extends HBox {

	private Order order;
	private double totalPrice;

	public OrderOverviewBox(Order order) {
		this.order = order;
		initialize();
		
		for (ShoppingItem item : order.getItems())
			totalPrice += item.getTotal();
	}

	/** Returns the order associated with this box. */
	public Order getOrder() {
		return this.order;
	}
	
	private void initialize() {
		this.getStyleClass().add("order-box");
		
		// Setup the box that holds the date
		VBox dateBox = new VBox();
		dateBox.setAlignment(Pos.CENTER);

		// The Date class is not very well-designed, using auxiliary method to
		// format date.
		int[] date = getUsableDate(order.getDate());
		int day = date[0];
		String month = order.getDate().toString().split(" ")[1];
		int year = date[2];

		Label lblDayAndMonth = new Label(String.format("%d %s", day, month));
		Label lblYear = new Label(Integer.toString(year));

		lblDayAndMonth.getStyleClass().add("label-order-overview-day-month");
		lblYear.getStyleClass().add("label-order-overview-year");

		dateBox.getChildren().add(lblDayAndMonth);
		dateBox.getChildren().add(lblYear);

		// Add the box that holds the date
		this.getChildren().add(dateBox);

		// Setup margins
		HBox.setMargin(dateBox, new Insets(0, 28, 0, 28));
	}
	
	/** The date class is absolute garbage. 
	 * Returns an array where [0] holds the day, [1] the month, and [2] the year. */
	private int[] getUsableDate(Date date) {
		String s[] = date.toString().split(" ");
		return new int[]{ Integer.parseInt(s[2]), monthToInt(s[1]), Integer.parseInt(s[s.length - 1]) };
	}
	
	private int monthToInt(String month) {
		String months[] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
		for (int i = 0; i < months.length; ++i) {
			if (month.equals(months[i]))
				return i + 1;
		}
		
		//Thow exception if argument is not included in list.
		throw new IllegalArgumentException("There is no month " + month);
	}
	
	public List<Node> getProductBoxes() {
		List<Node> productBoxes = new ArrayList<Node>();
		for (ShoppingItem item : order.getItems())
			productBoxes.add(new ItemInHistoryBox(item));
		
		return productBoxes;
	}
	
	public String getDate() {
		int[] date = getUsableDate(order.getDate());
		int day = date[0];
		int month = monthToInt(order.getDate().toString().split(" ")[1]);
		int year = date[2];
		
		return String.format("%d/%d/%d", day, month, year);
	}
	
	public double getTotalPrice() {
		return this.totalPrice;
	}
}
