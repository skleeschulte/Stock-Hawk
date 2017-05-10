package com.udacity.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by Stefan on 10.05.2017.
 */

public class StockListWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockListRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    class StockListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private Context mContext;
        private int mAppWidgetId;
        private Cursor mData;

        private final DecimalFormat dollarFormat;

        public StockListRemoteViewsFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        }

        @Override
        public void onCreate() {
            // Nothing to do
        }

        @Override
        public void onDataSetChanged() {
            if (mData != null) {
                mData.close();
            }
            // This method is called by the app hosting the widget (e.g., the launcher)
            // However, our ContentProvider is not exported so it doesn't have access to the
            // data. Therefore we need to clear (and finally restore) the calling identity so
            // that calls use our process and permission
            final long identityToken = Binder.clearCallingIdentity();

            mData = getContentResolver().query(Contract.Quote.URI,
                    new String[] { Contract.Quote._ID, Contract.Quote.COLUMN_SYMBOL, Contract.Quote.COLUMN_PRICE },
                    null,
                    null,
                    Contract.Quote.COLUMN_SYMBOL);

            Binder.restoreCallingIdentity(identityToken);
        }

        @Override
        public void onDestroy() {
            if (mData != null) {
                mData.close();
                mData = null;
            }
        }

        @Override
        public int getCount() {
            return mData == null ? 0 : mData.getCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position == AdapterView.INVALID_POSITION ||
                    mData == null || !mData.moveToPosition(position)) {
                return null;
            }

            String symbol = mData.getString(mData.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
            float price = mData.getFloat(mData.getColumnIndex(Contract.Quote.COLUMN_PRICE));

            RemoteViews views = new RemoteViews(getPackageName(), R.layout.stock_list_widget_item);
            views.setTextViewText(R.id.symbol, symbol);
            views.setTextViewText(R.id.price, dollarFormat.format(price));

            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(getPackageName(), R.layout.stock_list_widget_item);
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            if (mData != null && mData.moveToPosition(position))
                return mData.getLong(mData.getColumnIndex(Contract.Quote._ID));
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

}
