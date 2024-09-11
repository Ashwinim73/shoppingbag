package com.example.shoppingbag;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoppingbag.Model.Cart;
import com.example.shoppingbag.Prevalent.Prevalent;
import com.example.shoppingbag.ViewHolder.CartViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CartActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private Button NextProcessBtn;
    private TextView txtTotalAmount,txtmsg1;
    private int overTotalPrice = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);
        recyclerView =findViewById(R.id.cart_list);
        recyclerView.setHasFixedSize(true);
        layoutManager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        NextProcessBtn =(Button) findViewById(R.id.next_btn);
        txtTotalAmount=(TextView) findViewById(R.id.page_title);
        txtmsg1=(TextView) findViewById(R.id.msg1);
        NextProcessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtTotalAmount.setText("Total price = \u20B9" + String.valueOf(overTotalPrice));
                Intent intent =new Intent(CartActivity.this, ConfirmFinalOrderActivity.class);
                intent.putExtra("Total Price",String.valueOf(overTotalPrice));
                startActivity(intent);
                finish();

            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        CheckOrderState();
        String user=Prevalent.currentOnlineUser.getPhone();
        final DatabaseReference cartListRef = FirebaseDatabase.getInstance().getReference().child("Cart List");
        FirebaseRecyclerOptions<Cart>options=
                new FirebaseRecyclerOptions.Builder<Cart>()
                        .setQuery(cartListRef.child("User View")
                                .child(user)
                                .child("Products"),Cart.class)
                                .build();
        FirebaseRecyclerAdapter<Cart, CartViewHolder> adapter
                = new FirebaseRecyclerAdapter<Cart, CartViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CartViewHolder cartViewHolder, int i, @NonNull Cart cart)
            {
                //cartViewHolder.txtProductQuantity.setText(cart.getQuantity());
                cartViewHolder.txtProductPrice.setText("\u20B9" +cart.getPrice());
                cartViewHolder.txtProductName.setText(cart.getPname());
                int oneTyprProductTPrice = ((Integer.valueOf(cart.getPrice())));
                overTotalPrice = overTotalPrice + oneTyprProductTPrice;
                cartViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        CharSequence options[] = new CharSequence[]
                                {
                                        "Remove"
                                };
                        AlertDialog.Builder builder = new AlertDialog.Builder(CartActivity.this);
                        builder.setTitle("Cart Options:");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                if (which==0)
                                {
                                    cartListRef.child("User View")
                                            .child(Prevalent.currentOnlineUser.getPhone())
                                            .child("Products")
                                            .child(cart.getPid())
                                            .removeValue()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        cartListRef.child("User View")
                                                                .child(Prevalent.currentOnlineUser.getPhone())
                                                                .child("Products")
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        if (!snapshot.exists()) {
                                                                            // Cart is empty, move to HomeActivity
                                                                            Intent intent = new Intent(CartActivity.this, HomeActivity.class);
                                                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear back stack
                                                                            startActivity(intent);
                                                                            finish(); // Finish current activity
                                                                        } else {
                                                                            // Cart is not empty
                                                                            Toast.makeText(CartActivity.this, "Item removed Successfully.", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                                    }

                                                                });


                                                    }
                                                }
                                            });

                                }
                            }
                        });
                        builder.show();

                    }
                });

            }

            @NonNull
            @Override
            public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_items_layout,parent,false);
                CartViewHolder cartViewHolder=new CartViewHolder(view);
                return cartViewHolder;

            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }
private  void CheckOrderState()
{
    DatabaseReference ordersRef;
    ordersRef = FirebaseDatabase.getInstance().getReference().child("Orders").child(Prevalent.currentOnlineUser.getPhone());
    ordersRef.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (snapshot.exists())
            {
                String shippingState = snapshot.child("state").getValue().toString();
                String userName=snapshot.child("name").getValue().toString();
                if (shippingState.equals("shipped"))
                {
                    txtTotalAmount.setText("Dear"+userName+"\n order is shipped successfully. ");
                    recyclerView.setVisibility(View.GONE);
                    txtmsg1.setVisibility(View.VISIBLE);
                    txtmsg1.setText("Congratulations, your final order has been Shipped successfully. Soon you will received your order at your door step.");
                    NextProcessBtn.setVisibility(View.GONE);

                    Toast.makeText(CartActivity.this, "you can purchase more products, once you received your first final order.", Toast.LENGTH_SHORT).show();
                }
                else if(shippingState.equals("not shipped"))
                {
                    txtTotalAmount.setText("Shipping State = Not Shipped");
                    recyclerView.setVisibility(View.GONE);

                    txtmsg1.setVisibility(View.VISIBLE);
                    NextProcessBtn.setVisibility(View.GONE);

                    Toast.makeText(CartActivity.this, "you can purchase more products, once you received your first final order.", Toast.LENGTH_SHORT).show();
                }
            }
        }





        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    });
}

}