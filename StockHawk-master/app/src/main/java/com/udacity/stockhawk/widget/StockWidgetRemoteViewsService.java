package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.icu.text.DecimalFormat;
import android.icu.text.NumberFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.util.Locale;

/**
 * RemoteViewsService controlling the data being shown in the scrollable stock widget
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class StockWidgetRemoteViewsService extends RemoteViewsService {

    final private String TAG = StockWidgetRemoteViewsService.class.getSimpleName();

    DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
    DecimalFormat formatChange = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                formatChange.setPositivePrefix("+$");
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                Uri stockUri = Contract.Quote.URI;
                data = getContentResolver().query(stockUri,
                        null,
                        Contract.Quote.COLUMN_HISTORY + " IS NOT NULL",
                        null,
                        Contract.Quote.COLUMN_SYMBOL);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }

                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_stock_list_item);

                String symbol = data.getString(data.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
                views.setTextViewText(R.id.widget_stock_list_item_symbol, symbol);

                final float rawAbsoluteChange =
                        data.getFloat(data.getColumnIndex(Contract.Quote.COLUMN_ABSOLUTE_CHANGE));
                String formattedChange = formatChange.format(rawAbsoluteChange);
                views.setTextViewText(R.id.widget_stock_list_item_change, formattedChange);

                float price = data.getFloat(data.getColumnIndex(Contract.Quote.COLUMN_PRICE));
                String formattedPrice = format.format(price);
                views.setTextViewText(R.id.widget_stock_list_item_price, formattedPrice);

                // Setting the background of the stock price change
                // depending on it's operational signal
                if (rawAbsoluteChange > 0) {
                    views.setInt(R.id.widget_stock_list_item_change, "setBackgroundResource",
                            R.drawable.percent_change_pill_green);
                } else {
                    views.setInt(R.id.widget_stock_list_item_change, "setBackgroundResource",
                            R.drawable.percent_change_pill_red);
                }

                final Intent fillInIntent = new Intent();
                Uri stockUri = Contract.Quote.makeUriForStock(symbol);
                fillInIntent.setData(stockUri);
                fillInIntent.putExtra("SYMBOL", symbol);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_stock_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(data.getColumnIndex(Contract.Quote._ID));
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
