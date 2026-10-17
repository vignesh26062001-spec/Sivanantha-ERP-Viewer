package com.sivanantha.erp;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final int navy=Color.rgb(14,32,59), gold=Color.rgb(184,140,61), muted=Color.rgb(104,118,137), bg=Color.rgb(246,248,251);
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private LinearLayout body, root;
    private JSONArray records=new JSONArray();
    private String token="", currentTable="", query="";
    private static final String VERSION="v1.5 • Ledgers Beta";
    private boolean loading=false;
    private static final String SESSION_KEY="sivanantha_viewer_session_v1";
    private static final String[] TABLES={"weavers","designs","yarn_challans","warp_challans","grey_challans","godown_inwards","yarn_purchases","supplier_payments","coolie_vouchers","weaver_advances","grey_quality","cancelled_documents","change_history"};
    private static final String[] LABELS={"Weaver Master","Design Master","Yarn Issue DC","Warp Issue DC","Grey Inward","Godown Inward","Yarn Purchases","Supplier Payments","Vouchers","Weaver Advances","Grey Quality","Cancelled Documents","Change History"};
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private void gap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private void pad(View v,int p){v.setPadding(dp(p),dp(p),dp(p),dp(p));}
    private void card(LinearLayout parent,View content){LinearLayout w=vertical();pad(w,16);w.setBackground(shape(Color.WHITE,16));w.setElevation(dp(2));w.addView(content);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);parent.addView(w,p);}
    private void btn(LinearLayout l,String label,Runnable action,boolean primary){TextView b=text(label,14,primary?Color.WHITE:navy,true);b.setGravity(Gravity.CENTER);pad(b,14);b.setBackground(shape(primary?navy:Color.rgb(234,238,244),12));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);l.addView(b,p);b.setOnClickListener(v->action.run());}
    private EditText field(LinearLayout l,String hint,String value,boolean password){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(14);e.setHint(hint);e.setText(value);e.setPadding(dp(12),dp(12),dp(12),dp(12));e.setBackground(shape(Color.rgb(239,242,247),10));if(password)e.setInputType(129);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);l.addView(e,p);return e;}
    @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("viewer",MODE_PRIVATE);try{records=new JSONArray(prefs.getString("cache","[]"));}catch(Exception ignored){}showHome();autoSyncOnStartup();}
    private void screen(String title,String subtitle){root=vertical();root.setBackgroundColor(bg);setContentView(root);LinearLayout head=vertical();pad(head,20);head.setBackgroundColor(navy);TextView brand=text("SIVANANTHA  /  EXPORT FABS",12,gold,true);head.addView(brand);gap(head,5);head.addView(text(VERSION,11,Color.WHITE,true));gap(head,12);head.addView(text(title,23,Color.WHITE,true));gap(head,4);head.addView(text(subtitle,12,Color.rgb(201,210,223),false));root.addView(head);ScrollView sc=new ScrollView(this);body=vertical();pad(body,17);sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));LinearLayout nav=new LinearLayout(this);nav.setBackgroundColor(Color.WHITE);String[] names={"⌂  Home","▦  Modules","↻  Refresh","⚙  Settings"};for(int i=0;i<4;i++){final int idx=i;TextView item=text(names[i],12,navy,true);item.setGravity(Gravity.CENTER);item.setPadding(0,dp(18),0,dp(18));nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));item.setOnClickListener(v->{if(idx==0)showHome();if(idx==1)showModules();if(idx==2)refresh();if(idx==3)showSettings();});}root.addView(nav);}
    private String status(){String time=prefs.getString("last_sync","");return time.isEmpty()?"No cloud download yet • offline cache available after first sync":"Last downloaded: "+time;}
    private int count(String table){int n=0;for(int i=0;i<records.length();i++)if(table.equals(records.optJSONObject(i).optString("source_table")))n++;return n;}
    private void showHome(){
        currentTable="";screen("Business dashboard",status());
        LinearLayout summary=vertical();summary.addView(text("SYNCHRONIZED BUSINESS RECORDS",12,gold,true));gap(summary,8);
        summary.addView(text(String.valueOf(records.length()),32,navy,true));
        summary.addView(text("Read-only • available offline after refresh",12,muted,false));card(body,summary);
        int[] featured={0,2,3,4,5,8};
        for(int idx:featured){final int j=idx;LinearLayout r=vertical();r.addView(text(LABELS[j],14,navy,true));gap(r,6);
            r.addView(text(count(TABLES[j])+" records",23,gold,true));card(body,r);
            body.getChildAt(body.getChildCount()-1).setOnClickListener(v->showTable(TABLES[j],LABELS[j],""));
        }
        btn(body,"Business Ledgers • Weaver / Supplier / Stock",this::showLedgerMenu,true);
        btn(body,"Weaver activity statements (PDF)",this::showWeaverStatements,false);
        btn(body,"Godown inward register (PDF)",()->createRegisterPdf("godown_inwards","GODOWN INWARD REGISTER"),false);
        btn(body,"Browse all modules",this::showModules,false);
        btn(body,"Refresh from Supabase",this::refresh,false);
        body.addView(text("Dashboard counts reflect downloaded records, not live inventory or financial balances.",12,muted,false));
    }
    private void showModules(){screen("Modules","Browse your synchronized business records");for(int i=0;i<TABLES.length;i++){final String table=TABLES[i], label=LABELS[i];TextView t=text(label+"    ·    "+count(table),15,navy,true);card(body,t);body.getChildAt(body.getChildCount()-1).setOnClickListener(v->showTable(table,label,""));}}

    // v1.5 ledger calculations use the same cumulative transaction rules as Windows ERP.
    // All results are based on the last successfully downloaded offline cache.
    private double num(JSONObject d,String k){return d==null?0:d.optDouble(k,0);}
    private String str(JSONObject d,String k){return d==null?"":d.optString(k,"");}
    private String refWeaver(JSONObject d){return str(d,"weaverId").isEmpty()?str(d,"weaver_id"):str(d,"weaverId");}
    private String cash(double n){return String.format(Locale.US,"%,.2f",n);}
    private String kg(double n){return String.format(Locale.US,"%,.3f",n);}
    private static class LedgerLine {
        String date,desc;double plus,minus,balance;
        LedgerLine(String d,String t,double p,double m){date=d;desc=t;plus=p;minus=m;}
    }
    private JSONObject weaverRow(String id){
        for(int i=0;i<records.length();i++){
            JSONObject r=records.optJSONObject(i);if(r==null||!"weavers".equals(r.optString("source_table")))continue;
            JSONObject d=r.optJSONObject("payload");if(d!=null&&id.equals(str(d,"id")))return d;
        }return new JSONObject();
    }
    private void showLedgerMenu(){
        screen("Business Ledgers","Read-only • balances from cached Windows transactions");
        btn(body,"Weaver Yarn Balance Ledger",()->chooseLedger("yarn"),true);
        btn(body,"Weaver Coolie Ledger",()->chooseLedger("coolie"),false);
        btn(body,"Weaver Advance Ledger",()->chooseLedger("advance"),false);
        btn(body,"Weaver Production Ledger",()->chooseLedger("production"),false);
        btn(body,"Yarn Supplier Ledger",this::chooseSupplierLedger,false);
        btn(body,"Godown Stock Ledger",()->displayLedger("stock","Godown Stock",0,stockLines()),false);
        body.addView(text("Balances are calculated from synchronized active records. Check against Windows ERP before official use. Godown stock is total weight, not count/color-wise stock.",12,muted,false));
    }
    private void chooseLedger(String kind){
        screen("Choose Weaver","Select a weaver for "+kind+" ledger");int n=0;
        for(int i=0;i<records.length();i++){
            JSONObject r=records.optJSONObject(i);if(r==null||!"weavers".equals(r.optString("source_table")))continue;
            JSONObject d=r.optJSONObject("payload");if(d==null)continue;
            String id=str(d,"id"),name=str(d,"name");
            if(id.isEmpty())id=r.optString("source_key");
            final String wid=id,wn=name;
            btn(body,name+"  •  "+kind,()->displayLedger(kind,wn,opening(kind,wid),weaverLines(kind,wid)),false);n++;
        }
        if(n==0)body.addView(text("No cached weavers found. Refresh online.",13,muted,false));
    }
    private double opening(String kind,String id){JSONObject w=weaverRow(id);return kind.equals("yarn")?num(w,"openYarn"):kind.equals("coolie")?num(w,"openCoolie"):0;}
    private ArrayList<LedgerLine> weaverLines(String kind,String id){
        ArrayList<LedgerLine> lines=new ArrayList<>();
        for(int i=0;i<records.length();i++){
            JSONObject r=records.optJSONObject(i);if(r==null)continue;JSONObject d=r.optJSONObject("payload");if(d==null||!id.equals(refWeaver(d)))continue;
            String t=r.optString("source_table"),date=str(d,"date"),doc=recordNumber(d);
            if(kind.equals("yarn")){
                if(t.equals("yarn_challans")||t.equals("warp_challans"))lines.add(new LedgerLine(date,labelFor(t)+" "+doc,num(d,"totalWt"),0));
                if(t.equals("grey_challans"))lines.add(new LedgerLine(date,"Grey Inward "+doc,0,num(d,"totalDeductWt")));
            }else if(kind.equals("coolie")){
                if(t.equals("grey_challans"))lines.add(new LedgerLine(date,"Grey Coolie "+doc,num(d,"totalCoolie"),num(d,"paid")));
                if(t.equals("coolie_vouchers"))lines.add(new LedgerLine(date,"Voucher "+doc,0,num(d,"amount")));
            }else if(kind.equals("advance")&&t.equals("weaver_advances")){
                double amt=num(d,"amount");boolean adv="Advance".equalsIgnoreCase(str(d,"kind"));
                lines.add(new LedgerLine(date,str(d,"kind")+" "+str(d,"reference"),adv?amt:0,adv?0:amt));
            }else if(kind.equals("production")&&t.equals("grey_challans")){
                lines.add(new LedgerLine(date,"Grey "+doc+" • "+str(d,"totalPcs")+" pcs",num(d,"totalMeters"),0));
            }
        }
        Collections.sort(lines,(a,b)->a.date.compareTo(b.date));return lines;
    }
    private void chooseSupplierLedger(){
        screen("Supplier Ledger","Choose supplier • purchases less payments");TreeSet<String> suppliers=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(int i=0;i<records.length();i++){
            JSONObject r=records.optJSONObject(i);if(r==null)continue;
            String t=r.optString("source_table");if(!t.equals("yarn_purchases")&&!t.equals("supplier_payments"))continue;
            String supplier=str(r.optJSONObject("payload"),"supplier");if(!supplier.isEmpty())suppliers.add(supplier);
        }
        for(String supplier:suppliers)btn(body,supplier,()->displayLedger("supplier",supplier,0,supplierLines(supplier)),false);
        if(suppliers.isEmpty())body.addView(text("No synchronized supplier records.",13,muted,false));
    }
    private ArrayList<LedgerLine> supplierLines(String supplier){
        ArrayList<LedgerLine> lines=new ArrayList<>();
        for(int i=0;i<records.length();i++){
            JSONObject r=records.optJSONObject(i);if(r==null)continue;JSONObject d=r.optJSONObject("payload");if(d==null||!supplier.equalsIgnoreCase(str(d,"supplier")))continue;
            String t=r.optString("source_table");
            if(t.equals("yarn_purchases"))lines.add(new LedgerLine(str(d,"date"),"Purchase "+str(d,"invoice_no"),num(d,"quantity_kg")*num(d,"rate_per_kg")+num(d,"extra_charges"),0));
            if(t.equals("supplier_payments"))lines.add(new LedgerLine(str(d,"date"),"Payment "+str(d,"reference"),0,num(d,"amount")));
        }Collections.sort(lines,(a,b)->a.date.compareTo(b.date));return lines;
    }
    private ArrayList<LedgerLine> stockLines(){
        ArrayList<LedgerLine> lines=new ArrayList<>();
        for(int i=0;i<records.length();i++){
            JSONObject r=records.optJSONObject(i);if(r==null)continue;JSONObject d=r.optJSONObject("payload");if(d==null)continue;
            String t=r.optString("source_table"),date=str(d,"date");
            if(t.equals("godown_inwards"))lines.add(new LedgerLine(date,"Inward "+recordNumber(d)+" "+str(d,"count")+" "+str(d,"color"),num(d,"netWt"),0));
            if(t.equals("yarn_challans")||t.equals("warp_challans"))lines.add(new LedgerLine(date,"Issue "+recordNumber(d),0,num(d,"totalWt")));
        }Collections.sort(lines,(a,b)->a.date.compareTo(b.date));return lines;
    }
    private void displayLedger(String kind,String name,double opening,ArrayList<LedgerLine> lines){
        String title=kind.equals("stock")?"Godown Stock Ledger":kind.equals("supplier")?"Supplier Ledger":("Weaver "+kind.substring(0,1).toUpperCase(Locale.US)+kind.substring(1)+" Ledger");
        String unit=kind.equals("yarn")||kind.equals("stock")?"kg":kind.equals("production")?"m":"₹";
        double balance=opening,totalIn=0,totalOut=0;
        for(LedgerLine line:lines){balance+=line.plus-line.minus;line.balance=balance;totalIn+=line.plus;totalOut+=line.minus;}
        final double finalBalance=balance,ti=totalIn,to=totalOut;
        screen(title,name+" • Offline synchronized records");
        LinearLayout summary=vertical();summary.addView(text(name,18,navy,true));gap(summary,7);
        summary.addView(text("Opening: "+unit+" "+cash(opening),13,muted,false));
        summary.addView(text("Total In / Credit: "+unit+" "+cash(ti),13,muted,false));
        summary.addView(text("Total Out / Debit: "+unit+" "+cash(to),13,muted,false));
        summary.addView(text("Closing balance: "+unit+" "+cash(finalBalance),19,gold,true));card(body,summary);
        btn(body,"GENERATE LEDGER PDF • SHARE / PRINT",()->ledgerPdf(title,name,unit,opening,lines,finalBalance),true);
        if(lines.isEmpty())body.addView(text("No matching transactions in offline cache.",13,muted,false));
        for(LedgerLine l:lines){LinearLayout item=vertical();item.addView(text(l.date+"  •  "+l.desc,13,navy,true));gap(item,6);
            item.addView(text("In: "+cash(l.plus)+"    Out: "+cash(l.minus)+"    Balance: "+cash(l.balance)+" "+unit,12,muted,false));card(body,item);}
    }
    private void ledgerPdf(String title,String name,String unit,double opening,ArrayList<LedgerLine> lines,double closing){
        try{
            pdfDoc=new PdfDocument();pdfPage=null;pdfPageNo=0;page(title.toUpperCase(Locale.US));
            para("Account: "+name+"  |  Opening: "+unit+" "+cash(opening),title);
            String[] heads={"Date","Transaction","In","Out","Balance"};float[] widths={84,177,80,80,86};
            tableHeader(heads,widths,title);
            for(LedgerLine l:lines)tableRow(new String[]{l.date,l.desc,cash(l.plus),cash(l.minus),cash(l.balance)},widths,title,heads);
            para("Closing balance: "+unit+" "+cash(closing),title);
            para("Source: Android last synced cache • Verify against Windows ERP",title);
            exportPdf("Ledger_"+title+"_"+name);
        }catch(Exception e){pdfFailure(e);}
    }

    private void showWeaverStatements(){
        screen("Weaver statements","Choose a weaver • PDF from cached transactions");
        int found=0;
        for(int i=0;i<records.length();i++){
            JSONObject row=records.optJSONObject(i);if(row==null||!"weavers".equals(row.optString("source_table")))continue;
            JSONObject d=row.optJSONObject("payload");if(d==null)continue;
            String id=d.optString("id",row.optString("source_key"));String name=d.optString("name","Weaver #"+id);
            final String selectedId=id,selectedName=name;
            btn(body,name+"  •  Activity PDF",()->createWeaverPdf(selectedId,selectedName),false);found++;
        }
        if(found==0)body.addView(text("No weavers cached. Refresh online first.",13,muted,false));
        body.addView(text("Activity statements list synced entries. They are not audited running-balance ledgers.",12,muted,false));
    }
    private void exportPdf(String filename){
        try{
            if(pdfPage!=null){pdfDoc.finishPage(pdfPage);pdfPage=null;}
            File folder=new File(getCacheDir(),"shared_pdfs");if(!folder.exists()&&!folder.mkdirs())throw new IOException("Unable to create PDF directory");
            File out=new File(folder,filename.replaceAll("[^A-Za-z0-9_-]","_")+".pdf");
            try(FileOutputStream stream=new FileOutputStream(out)){pdfDoc.writeTo(stream);}
            pdfDoc.close();pdfDoc=null;
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",out);
            Intent share=new Intent(Intent.ACTION_SEND);share.setType("application/pdf");share.putExtra(Intent.EXTRA_STREAM,uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share,"Save, print or share PDF"));
        }catch(Exception e){pdfFailure(e);}
    }
    private void pdfFailure(Exception e){
        if(pdfDoc!=null){try{if(pdfPage!=null)pdfDoc.finishPage(pdfPage);pdfDoc.close();}catch(Exception ignored){}}
        pdfDoc=null;pdfPage=null;
        new AlertDialog.Builder(this).setTitle("PDF generation failed").setMessage(e.getMessage()).setPositiveButton("OK",null).show();
    }
    private void createWeaverPdf(String weaverId,String name){
        try{
            String subtitle="WEAVER ACTIVITY STATEMENT";
            pdfDoc=new PdfDocument();pdfPage=null;pdfPageNo=0;page(subtitle);
            para("Weaver: "+name+"  |  ID: "+weaverId,subtitle);
            para("Synced activity only • no calculated opening/closing balance",subtitle);
            String[] heads={"Date","Module","Document","Quantity / Amount"};float[] widths={87,137,132,151};
            tableHeader(heads,widths,subtitle);int n=0;
            for(int i=0;i<records.length();i++){
                JSONObject r=records.optJSONObject(i);if(r==null)continue;
                JSONObject d=r.optJSONObject("payload");if(d==null)continue;
                String table=r.optString("source_table");if(table.equals("weavers")||!weaverId.equals(d.optString("weaverId",d.optString("weaver_id"))))continue;
                String qty=first(d,"totalWt","totalMeters","amount","totalCoolie","netWt");
                tableRow(new String[]{val(d,"date"),labelFor(table),recordNumber(d),qty},widths,subtitle,heads);n++;
            }
            if(n==0)para("No matching synced transactions found.",subtitle);
            para("Entries: "+n+"  |  Source: Android offline cache",subtitle);
            exportPdf("Weaver_Activity_"+weaverId);
        }catch(Exception e){pdfFailure(e);}
    }
    private void createRegisterPdf(String table,String subtitle){
        try{
            pdfDoc=new PdfDocument();pdfPage=null;pdfPageNo=0;page(subtitle);
            para("Synced entries only • not a calculated live stock balance",subtitle);
            String[] heads={"Date","Document","Weaver","Quantity / Weight"};float[] widths={95,128,139,145};
            tableHeader(heads,widths,subtitle);int n=0;
            for(int i=0;i<records.length();i++){
                JSONObject r=records.optJSONObject(i);if(r==null||!table.equals(r.optString("source_table")))continue;
                JSONObject d=r.optJSONObject("payload");if(d==null)continue;
                String w=weaverName(d.optString("weaverId"));
                tableRow(new String[]{val(d,"date"),recordNumber(d),w,first(d,"totalWt","netWt","totalMeters","totalPcs")},widths,subtitle,heads);n++;
            }
            if(n==0)para("No cached entries found.",subtitle);
            para("Total entries: "+n,subtitle);exportPdf("Godown_Inward_Register");
        }catch(Exception e){pdfFailure(e);}
    }
    private String labelFor(String table){for(int i=0;i<TABLES.length;i++)if(TABLES[i].equals(table))return LABELS[i];return table;}
    private String niceLabel(String key){
        String k=key.replaceAll("([a-z])([A-Z])","$1 $2").replace('_',' ').trim();
        String lower=k.toLowerCase(Locale.ROOT);
        if(lower.equals("id"))return "Record ID";
        if(lower.equals("weaver id"))return "Weaver";
        if(lower.equals("challan no"))return "DC Number";
        if(lower.equals("inward no"))return "Inward Number";
        if(lower.equals("vch no"))return "Voucher Number";
        if(lower.equals("total wt")||lower.equals("net wt"))return lower.equals("net wt")?"Net Weight (kg)":"Total Weight (kg)";
        if(lower.equals("total meters"))return "Total Length (m)";
        if(lower.equals("total pcs"))return "Total Pieces";
        if(lower.equals("total coolie"))return "Total Weaving Charges";
        if(lower.equals("open yarn"))return "Opening Yarn";
        if(lower.equals("open coolie"))return "Opening Weaving Charges";
        if(lower.equals("items json"))return "DC Line Items";
        String[] words=k.split(" ");StringBuilder out=new StringBuilder();
        for(String w:words){if(w.isEmpty())continue;if(out.length()>0)out.append(' ');out.append(w.equalsIgnoreCase("dc")?"DC":Character.toUpperCase(w.charAt(0))+w.substring(1));}
        return out.toString();
    }
    private String valueText(String key,Object value){
        if(value==null||value==JSONObject.NULL)return "—";
        String raw=String.valueOf(value);
        if(raw.trim().isEmpty()||raw.equalsIgnoreCase("null"))return "—";
        if(key.equals("weaverId")){String found=weaverName(raw);if(!found.isEmpty())return found+"  (#"+raw+")";}
        if(value instanceof JSONArray){JSONArray ar=(JSONArray)value;return ar.length()+" line item"+(ar.length()==1?"":"s");}
        if(value instanceof JSONObject)return "View details below";
        if(key.toLowerCase(Locale.ROOT).contains("json")){try{JSONArray a=new JSONArray(raw);return a.length()+" line item"+(a.length()==1?"":"s");}catch(Exception ignored){}}
        if((key.equals("date")||key.toLowerCase(Locale.ROOT).endsWith("date"))&&raw.matches("\\d{4}-\\d{2}-\\d{2}.*")){
            try{java.text.DateFormat from=new SimpleDateFormat("yyyy-MM-dd",Locale.US);java.text.DateFormat to=new SimpleDateFormat("dd MMM yyyy",Locale.getDefault());return to.format(from.parse(raw.substring(0,10)));}catch(Exception ignored){}
        }
        if(key.equals("amount")||key.equals("paid")||key.equals("totalCoolie")||key.equals("coolie")||key.equals("openCoolie")){
            try{return "₹"+String.format(new Locale("en","IN"),"%,.2f",Double.parseDouble(raw));}catch(Exception ignored){}
        }
        return raw;
    }
    private String weaverName(String id){
        for(int i=0;i<records.length();i++){JSONObject r=records.optJSONObject(i);if(r==null||!"weavers".equals(r.optString("source_table")))continue;JSONObject d=r.optJSONObject("payload");if(d!=null&&id.equals(d.optString("id")))return d.optString("name","");}return "";
    }
    private String first(JSONObject d,String... keys){for(String k:keys){String v=valueText(k,d.opt(k));if(!v.equals("—"))return v;}return "";}
    private String bestName(JSONObject d,String fallback){
        String name=first(d,"name","weaver_name","design_name","supplier","supplier_name","challanNo","inwardNo","vchNo","billNo","dc_no","challan_no","voucher_no");
        if(!name.isEmpty())return name;
        return "Record #"+fallback;
    }
    private String subtitleFor(JSONObject d){
        ArrayList<String> parts=new ArrayList<>();
        String date=first(d,"date","created_at");if(!date.isEmpty())parts.add(date);
        String w=first(d,"weaverId");if(!w.isEmpty())parts.add("Weaver: "+w);
        String supplier=first(d,"supplier");if(!supplier.isEmpty())parts.add(supplier);
        String wt=first(d,"totalWt","netWt");if(!wt.isEmpty())parts.add(wt+" kg");
        String length=first(d,"totalMeters");if(!length.isEmpty())parts.add(length+" m");
        String amount=first(d,"amount","totalCoolie");if(!amount.isEmpty())parts.add(amount);
        String phone=first(d,"phone");if(!phone.isEmpty())parts.add(phone);
        if(parts.isEmpty()){
            Iterator<String> it=d.keys();while(it.hasNext()&&parts.size()<2){String k=it.next();if(k.equals("id")||k.endsWith("_json")||k.equals("items_json"))continue;String val=valueText(k,d.opt(k));if(!val.equals("—"))parts.add(niceLabel(k)+": "+val);}
        }
        return android.text.TextUtils.join("   •   ",parts);
    }
    private void showTable(String table,String title,String filter){
        currentTable=table;query=filter;screen(title,"READ ONLY  •  "+count(table)+" synced records");
        TextView hint=text("Search by name, DC number, date or other details",12,muted,false);body.addView(hint);gap(body,8);
        EditText search=field(body,"Search "+title,filter,false);
        search.setSingleLine(true);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v,action,event)->{showTable(table,title,search.getText().toString().trim());return true;});
        btn(body,"Search records",()->showTable(table,title,search.getText().toString().trim()),true);
        int shown=0;
        for(int i=0;i<records.length();i++){
            JSONObject row=records.optJSONObject(i);if(row==null||!table.equals(row.optString("source_table")))continue;
            JSONObject data=row.optJSONObject("payload");if(data==null)continue;
            if(!filter.isEmpty()&&!data.toString().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)))continue;
            String primary=bestName(data,row.optString("source_key"));
            LinearLayout c=vertical();
            TextView titleView=text(primary,17,navy,true);c.addView(titleView);gap(c,9);
            String sub=subtitleFor(data);if(!sub.isEmpty()){TextView st=text(sub,13,muted,false);st.setLineSpacing(dp(3),1f);c.addView(st);gap(c,8);}
            TextView more=text("VIEW DETAILS   ›",11,gold,true);c.addView(more);
            card(body,c);body.getChildAt(body.getChildCount()-1).setOnClickListener(v->showDetail(table,title,primary,data,filter));shown++;
        }
        if(shown==0){LinearLayout empty=vertical();empty.addView(text("No records found",17,navy,true));gap(empty,6);empty.addView(text("Try a different search or refresh from Supabase.",13,muted,false));card(body,empty);}
    }
    private void addDetailField(LinearLayout parent,String key,Object value){
        if(value==null||value==JSONObject.NULL)return;
        String display=valueText(key,value);if(display.equals("—"))return;
        LinearLayout row=vertical();
        TextView label=text(niceLabel(key).toUpperCase(Locale.ROOT),11,muted,true);row.addView(label);gap(row,5);
        TextView val=text(display,16,navy,false);val.setTextIsSelectable(true);row.addView(val);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(17);parent.addView(row,lp);
    }
    private boolean isItemsKey(String key){
        String k=key.replace("_","").toLowerCase(Locale.ROOT);
        return k.equals("itemsjson")||k.equals("items")||k.equals("lineitems")||k.equals("lineitemsjson");
    }
    private JSONArray getItems(JSONObject data){
        Iterator<String> keys=data.keys();
        while(keys.hasNext()){
            String key=keys.next();if(!isItemsKey(key))continue;
            Object raw=data.opt(key);
            try{
                if(raw instanceof JSONArray)return (JSONArray)raw;
                if(raw instanceof String)return new JSONArray((String)raw);
            }catch(Exception ignored){}
        }
        return new JSONArray();
    }
    private String itemValue(JSONObject item,String key){
        Object v=item.opt(key);return v==null||v==JSONObject.NULL?"—":String.valueOf(v);
    }
    private void addLineItems(LinearLayout parent,JSONObject data){
        JSONArray lines=getItems(data);
        TextView heading=text("ITEM DETAILS  •  "+lines.length()+" ITEMS",15,gold,true);
        parent.addView(heading);gap(parent,12);
        if(lines.length()==0){
            TextView empty=text("No structured line items available for this record.",13,muted,false);
            parent.addView(empty);return;
        }
        for(int i=0;i<lines.length();i++){
            JSONObject item=lines.optJSONObject(i);if(item==null)continue;
            LinearLayout block=vertical();
            String material=item.optString("material",item.optString("designName","Item"));
            String color=item.optString("color","");
            block.addView(text("ITEM "+(i+1)+"  •  "+material+(color.isEmpty()?"":" — "+color),15,navy,true));gap(block,10);
            if(item.has("netWt"))addDetailField(block,"netWt",item.opt("netWt"));
            if(item.has("count"))addDetailField(block,"count",item.opt("count"));
            if(item.has("unitType"))addDetailField(block,"unitType",item.opt("unitType"));
            if(item.has("bags"))addDetailField(block,"bags",item.opt("bags"));
            if(item.has("perUnit"))addDetailField(block,"perUnit",item.opt("perUnit"));
            if(item.has("loose"))addDetailField(block,"loose",item.opt("loose"));
            Iterator<String> fields=item.keys();while(fields.hasNext()){
                String key=fields.next();
                if(Arrays.asList("material","color","netWt","count","unitType","bags","perUnit","loose").contains(key))continue;
                Object v=item.opt(key);if(v instanceof JSONObject||v instanceof JSONArray)continue;
                addDetailField(block,key,v);
            }
            card(parent,block);
        }
    }
    private void showDetail(String table,String title,String name,JSONObject data,String filter){
        currentTable=table;query=filter;
        String document=first(data,"challanNo","inwardNo","vchNo","name");
        screen(document.isEmpty()?name:document,title+"  •  READ ONLY");
        // Always visible before the scrollable detail fields: no need to hunt for the PDF button.
        TextView pdfHeading=text("PDF REPORT  •  v1.3",13,gold,true);
        body.addView(pdfHeading);gap(body,8);
        btn(body,"GENERATE PDF  •  SAVE / SHARE / PRINT",()->createPdf(table,data),true);
        btn(body,"‹  Back to "+title,()->showTable(table,title,filter),false);
        LinearLayout info=vertical();
        String[] priority={"challanNo","inwardNo","vchNo","date","name","phone","weaverId","supplier","design_name","looms","totalWt","totalMeters","totalPcs","netWt","amount","totalCoolie","paid"};
        Set<String> used=new HashSet<>();
        for(String key:priority){if(data.has(key)){addDetailField(info,key,data.opt(key));used.add(key);}}
        Iterator<String> keys=data.keys();while(keys.hasNext()){
            String key=keys.next();
            if(used.contains(key)||isItemsKey(key)||key.equals("id")||key.toLowerCase(Locale.ROOT).endsWith("json"))continue;
            Object v=data.opt(key);if(v instanceof JSONObject||v instanceof JSONArray)continue;
            addDetailField(info,key,v);
        }
        if(info.getChildCount()>0)card(body,info);
        if(table.equals("yarn_challans")||table.equals("warp_challans")||table.equals("grey_challans")||getItems(data).length()>0){
            addLineItems(body,data);
        }
        gap(body,12);
        btn(body,"GENERATE PDF  •  SAVE / SHARE / PRINT",()->createPdf(table,data),true);
        TextView footer=text("Sivanantha ERP Viewer v1.3  •  Cloud-synced read only",12,muted,false);
        footer.setGravity(Gravity.CENTER);body.addView(footer);
    }

    // A4 PDF layout based on Windows ERP v4.7 HTML templates. All values come from cached, read-only records.
    private static final int PDF_W=595, PDF_H=842;
    private Paint pdfPaint=new Paint(3);
    private PdfDocument pdfDoc;
    private PdfDocument.Page pdfPage;
    private Canvas pdfCanvas;
    private int pdfY, pdfPageNo;
    private void ink(int color,float size,boolean bold){pdfPaint.setColor(color);pdfPaint.setTextSize(size);pdfPaint.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));pdfPaint.setStyle(Paint.Style.FILL);}
    private void line(float x,float y,float xx,float yy){pdfPaint.setColor(Color.rgb(100,116,139));pdfPaint.setStrokeWidth(.65f);pdfCanvas.drawLine(x,y,xx,yy,pdfPaint);}
    private void txt(String s,float x,float y,int size,boolean bold){ink(navy,size,bold);pdfCanvas.drawText(s==null?"":s,x,y,pdfPaint);}
    private String clean(Object obj){if(obj==null||obj==JSONObject.NULL)return "";return String.valueOf(obj).replace('\n',' ').replace('\r',' ');}
    private String val(JSONObject j,String key){return clean(j.opt(key));}
    private String number(JSONObject j,String key,int places){try{return String.format(Locale.US,"%."+places+"f",Double.parseDouble(val(j,key).isEmpty()?"0":val(j,key)));}catch(Exception e){return val(j,key);}}
    private String fmt(JSONObject j,String key,int places){try{return String.format(Locale.US,"%."+places+"f",Double.parseDouble(val(j,key).isEmpty()?"0":val(j,key)));}catch(Exception e){return val(j,key);}}
    private String fit(String s,float width,int size){if(s==null)return "";s=s.replace('\n',' ').replace('\r',' ');ink(navy,size,false);if(pdfPaint.measureText(s)<=width)return s;while(s.length()>1&&pdfPaint.measureText(s+"…")>width)s=s.substring(0,s.length()-1);return s+"…";}
    private void page(String subtitle){
        if(pdfPage!=null)pdfDoc.finishPage(pdfPage);
        pdfPageNo++;PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(PDF_W,PDF_H,pdfPageNo).create();pdfPage=pdfDoc.startPage(info);pdfCanvas=pdfPage.getCanvas();
        pdfPaint.setStyle(Paint.Style.STROKE);pdfPaint.setColor(Color.BLACK);pdfPaint.setStrokeWidth(1.5f);pdfCanvas.drawRoundRect(29,29,566,813,5,5,pdfPaint);pdfPaint.setStyle(Paint.Style.FILL);
        try{Bitmap logo=BitmapFactory.decodeResource(getResources(),R.drawable.company_logo);if(logo!=null){pdfPaint.setAlpha(17);pdfCanvas.drawBitmap(logo,null,new android.graphics.Rect(192,320,403,531),pdfPaint);pdfPaint.setAlpha(255);}}catch(Exception ignored){}
        ink(Color.rgb(30,58,138),19,true);String name="SIVANANTHA EXPORT FABS";pdfCanvas.drawText(name,(PDF_W-pdfPaint.measureText(name))/2,72,pdfPaint);
        ink(Color.rgb(51,65,85),10,true);pdfCanvas.drawText(fit(subtitle,500,10),45,98,pdfPaint);line(44,107,551,107);pdfY=127;
    }
    private void ensure(int height,String subtitle){if(pdfY+height>768){page(subtitle+" (continued)");}}
    private void para(String text,String subtitle){ensure(35,subtitle);txt(fit(text,495,10),46,pdfY,10,false);pdfY+=23;}
    private void tableHeader(String[] heads,float[] widths,String subtitle){ensure(36,subtitle);pdfPaint.setColor(Color.rgb(226,232,240));pdfCanvas.drawRect(44,pdfY-15,551,pdfY+15,pdfPaint);float x=47;for(int i=0;i<heads.length;i++){txt(fit(heads[i],widths[i]-6,9),x,pdfY+3,9,true);x+=widths[i];}line(44,pdfY+16,551,pdfY+16);pdfY+=29;}
    private void tableRow(String[] cols,float[] widths,String subtitle,String[] heads){if(pdfY+26>762){page(subtitle+" (continued)");tableHeader(heads,widths,subtitle);}float x=47;for(int i=0;i<cols.length;i++){txt(fit(cols[i],widths[i]-7,9),x,pdfY,9,false);x+=widths[i];}line(44,pdfY+8,551,pdfY+8);pdfY+=25;}
    private void total(String text,String subtitle){ensure(60,subtitle);pdfY+=10;pdfPaint.setColor(Color.rgb(248,250,252));pdfCanvas.drawRect(44,pdfY-13,551,pdfY+22,pdfPaint);pdfPaint.setStyle(Paint.Style.STROKE);pdfPaint.setColor(Color.BLACK);pdfPaint.setStrokeWidth(1);pdfCanvas.drawRect(44,pdfY-13,551,pdfY+22,pdfPaint);pdfPaint.setStyle(Paint.Style.FILL);txt(fit(text,486,12),54,pdfY+8,12,true);pdfY+=48;}
    private void signatures(String subtitle){ensure(80,subtitle);pdfY+=35;txt("Weaver / Receiver Signature",45,pdfY,10,true);txt("For SIVANANTHA EXPORT FABS",355,pdfY,10,true);}
    private JSONArray items(JSONObject data){return getItems(data);}
    private String recordNumber(JSONObject d){String n=first(d,"challanNo","vchNo","inwardNo","invoice_no","invoiceNo","id");return n.isEmpty()?"Record":n;}
    private void createPdf(String table,JSONObject d){
        try{
            String subtitle;String[] heads=null;float[] widths=null;
            if(table.equals("yarn_challans")){subtitle="YARN ISSUE DELIVERY CHALLAN";heads=new String[]{"#","Count & Material","Color","Unit / Pack","Net Weight"};widths=new float[]{24,160,105,117,101};}
            else if(table.equals("warp_challans")){subtitle="WARP ISSUE DELIVERY CHALLAN";heads=new String[]{"#","Design","Main Warp","Border Warp","Meters","Weight"};widths=new float[]{22,117,111,111,69,77};}
            else if(table.equals("grey_challans")){subtitle="GREY STOCK INWARD SLIP";heads=new String[]{"#","Design","Pcs","Meters","Actual Wt","Deduct Wt","Rate","Coolie"};widths=new float[]{19,105,38,63,76,80,51,75};}
            else if(table.equals("coolie_vouchers"))subtitle="WEAVER COOLIE VOUCHER";
            else subtitle=labelFor(table).toUpperCase(Locale.ROOT)+" / RECORD COPY";
            pdfDoc=new PdfDocument();pdfPage=null;pdfPageNo=0;page(subtitle);
            String weaver=weaverName(val(d,"weaverId"));
            para("Document No: "+recordNumber(d)+"     |     Date: "+val(d,"date"),subtitle);
            if(!weaver.isEmpty())para("Weaver: "+weaver,subtitle);
            if(heads!=null){
                tableHeader(heads,widths,subtitle);JSONArray arr=items(d);
                for(int i=0;i<arr.length();i++){JSONObject it=arr.optJSONObject(i);if(it==null)continue;String[] cols;
                    if(table.equals("yarn_challans"))cols=new String[]{""+(i+1),val(it,"count")+" "+val(it,"material"),val(it,"color"),val(it,"bags")+" "+val(it,"unitType"),fmt(it,"netWt",3)+" kg"};
                    else if(table.equals("warp_challans"))cols=new String[]{""+(i+1),val(it,"designName"),val(it,"mainCount")+" ("+fmt(it,"mainWt",3)+")",val(it,"borderCount")+" ("+fmt(it,"borderWt",3)+")",val(it,"meters"),fmt(it,"totalWt",3)};
                    else cols=new String[]{""+(i+1),val(it,"designName"),val(it,"pcs"),fmt(it,"meters",1),fmt(it,"actualWt",3),fmt(it,"deductWt",3),val(it,"rate"),fmt(it,"coolie",2)};
                    tableRow(cols,widths,subtitle,heads);
                }
                if(table.equals("yarn_challans"))total("TOTAL YARN ISSUED: "+fmt(d,"totalWt",3)+" kg",subtitle);
                else if(table.equals("warp_challans"))total("TOTAL WARP ISSUED: "+fmt(d,"totalMeters",1)+" Mtrs  |  "+fmt(d,"totalWt",3)+" kg",subtitle);
                else total("TOTAL YARN DEDUCTED: "+fmt(d,"totalDeductWt",3)+" kg   |   COOLIE: INR "+fmt(d,"totalCoolie",2),subtitle);
            }else if(table.equals("coolie_vouchers")){
                para("Weaver Name: "+weaver+"     |     Payment Mode: "+val(d,"mode"),subtitle);
                para("Voucher Type: "+val(d,"type")+"     |     Remarks: "+val(d,"remarks"),subtitle);
                total("PAID AMOUNT: INR "+fmt(d,"amount",2),subtitle);
            }else{
                Iterator<String> keys=d.keys();while(keys.hasNext()){String k=keys.next();if(isItemsKey(k))continue;String v=val(d,k);if(!v.isEmpty())para(niceLabel(k)+": "+v,subtitle);}
                JSONArray arr=items(d);for(int i=0;i<arr.length();i++){JSONObject it=arr.optJSONObject(i);if(it!=null)para("Item "+(i+1)+": "+it.toString(),subtitle);}
            }
            signatures(subtitle);pdfDoc.finishPage(pdfPage);pdfPage=null;
            File folder=new File(getCacheDir(),"shared_pdfs");if(!folder.exists()&&!folder.mkdirs())throw new IOException("Unable to create PDF directory");
            String safe=(labelFor(table)+"_"+recordNumber(d)).replaceAll("[^A-Za-z0-9_-]","_");File out=new File(folder,safe+".pdf");try(FileOutputStream stream=new FileOutputStream(out)){pdfDoc.writeTo(stream);}pdfDoc.close();pdfDoc=null;
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",out);
            Intent share=new Intent(Intent.ACTION_SEND);share.setType("application/pdf");share.putExtra(Intent.EXTRA_STREAM,uri);share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share,"Save, print or share PDF"));
        }catch(Exception ex){if(pdfDoc!=null){try{if(pdfPage!=null)pdfDoc.finishPage(pdfPage);pdfDoc.close();}catch(Exception ignored){}}pdfDoc=null;pdfPage=null;new AlertDialog.Builder(this).setTitle("PDF generation failed").setMessage(ex.getMessage()).setPositiveButton("OK",null).show();}
    }

    private void showSettings(){
        screen("Cloud settings","Automatic secure sign-in & sync");
        body.addView(text("After the first sign-in, the app securely remembers your session and refreshes automatically on startup. Your password is never stored.",13,muted,false));
        gap(body,14);
        EditText url=field(body,"Project URL: https://xxxx.supabase.co",prefs.getString("url",""),false);
        EditText key=field(body,"Supabase publishable / anon key",prefs.getString("key",""),false);
        EditText email=field(body,"Android viewer email",prefs.getString("email",""),false);
        EditText password=field(body,"Viewer password (first sign-in only)","",true);
        btn(body,"Save connection & sign in",()->{
            String u=url.getText().toString().trim().replaceAll("/+$","");
            String k=key.getText().toString().trim(),e=email.getText().toString().trim(),pw=password.getText().toString();
            if(!u.matches("https://[A-Za-z0-9-]+\\.supabase\\.co")||k.isEmpty()||e.isEmpty()||pw.isEmpty()){toast("Check URL, key, email and password");return;}
            String oldIdentity=prefs.getString("url","")+"|"+prefs.getString("email","");
            if(!oldIdentity.equals(u+"|"+e))clearSession();
            prefs.edit().putString("url",u).putString("key",k).putString("email",e).apply();
            password.setText("");login(pw);
        },true);
        btn(body,"Sign out (keep offline records)",()->{clearSession();showHome();toast("Signed out. Offline records retained.");},false);
        btn(body,"Clear cached business data",()->new AlertDialog.Builder(this).setTitle("Clear offline cache?").setMessage("This removes downloaded records from this phone, not from Supabase.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{prefs.edit().remove("cache").remove("last_sync").apply();records=new JSONArray();showHome();}).show(),false);
    }
    private SecretKey sessionKey() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(!ks.containsAlias(SESSION_KEY)){
            KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(SESSION_KEY,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            generator.generateKey();
        }
        return (SecretKey)ks.getKey(SESSION_KEY,null);
    }
    private void saveSession(String refreshToken) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,sessionKey());
        byte[] encrypted=cipher.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));
        prefs.edit().putString("session_iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
            .putString("session_data",Base64.encodeToString(encrypted,Base64.NO_WRAP)).apply();
    }
    private String loadSession() throws Exception {
        String iv=prefs.getString("session_iv",""),data=prefs.getString("session_data","");
        if(iv.isEmpty()||data.isEmpty())return "";
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,sessionKey(),new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(data,Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }
    private void clearSession(){token="";prefs.edit().remove("session_iv").remove("session_data").apply();}
    private JSONObject authRequest(String grant,String value) throws Exception {
        String base=prefs.getString("url","");
        HttpURLConnection c=conn(base+"/auth/v1/token?grant_type="+grant,"POST","");
        c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
        JSONObject data=new JSONObject();
        if("password".equals(grant)){data.put("email",prefs.getString("email",""));data.put("password",value);}
        else data.put("refresh_token",value);
        try(OutputStream out=c.getOutputStream()){out.write(data.toString().getBytes(StandardCharsets.UTF_8));}
        String response=read(c);int code=c.getResponseCode();c.disconnect();
        if(code>=300)throw new IOException("Authentication HTTP "+code);
        return new JSONObject(response);
    }
    private void acceptSession(JSONObject session) throws Exception {
        String access=session.getString("access_token"),refresh=session.getString("refresh_token");
        saveSession(refresh);token=access;
    }
    private void autoSyncOnStartup(){
        if(prefs.getString("url","").isEmpty()||prefs.getString("session_data","").isEmpty())return;
        refresh();
    }
    private boolean renewSession() throws Exception {
        String saved=loadSession();if(saved.isEmpty())return false;
        acceptSession(authRequest("refresh_token",saved));return true;
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private String read(HttpURLConnection c)throws Exception{InputStream in=(c.getResponseCode()<400)?c.getInputStream():c.getErrorStream();if(in==null)return "";ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);in.close();return out.toString("UTF-8");}
    private HttpURLConnection conn(String endpoint,String method,String bearer)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestMethod(method);c.setRequestProperty("apikey",prefs.getString("key",""));c.setRequestProperty("Accept","application/json");if(!bearer.isEmpty())c.setRequestProperty("Authorization","Bearer "+bearer);return c;}
    private void login(String password){
        if(loading)return;loading=true;toast("Signing in…");
        io.execute(()->{try{
            acceptSession(authRequest("password",password));
            runOnUiThread(()->{loading=false;refresh();});
        }catch(Exception ex){runOnUiThread(()->{loading=false;toast("Login error: "+ex.getMessage());});}});
    }
    private void refresh(){
        if(loading)return;
        if(token.isEmpty()&&prefs.getString("session_data","").isEmpty()){
            showSettings();toast("Sign in once to enable automatic sync. Offline records are available.");return;
        }
        loading=true;
        io.execute(()->{
            try{
                // Refresh access token at every sync. Rotated refresh tokens are saved securely.
                try{
                    if(!renewSession())throw new IOException("No saved session");
                }catch(Exception authError){
                    // Do not discard saved credentials on network errors or timeouts.
                    String message=authError.getMessage()==null?"":authError.getMessage();
                    if(message.contains("Authentication HTTP 400")||message.contains("Authentication HTTP 401")||message.contains("Authentication HTTP 403")){
                        clearSession();throw new IOException("Session expired. Sign in again in Settings.");
                    }
                    throw authError;
                }
                JSONArray all=new JSONArray();String base=prefs.getString("url","");
                for(int offset=0;offset<100000;offset+=500){
                    String endpoint=base+"/rest/v1/erp_records?select=source_table,source_key,payload,is_deleted&is_deleted=eq.false&order=source_table.asc,source_key.asc&limit=500&offset="+offset;
                    HttpURLConnection c=conn(endpoint,"GET",token);String response=read(c);int code=c.getResponseCode();c.disconnect();
                    if(code>=300)throw new IOException("HTTP "+code+": "+response.substring(0,Math.min(180,response.length())));
                    JSONArray batch=new JSONArray(response);
                    for(int i=0;i<batch.length();i++)all.put(batch.getJSONObject(i));
                    if(batch.length()<500)break;
                    if(offset>=99500)throw new IOException("Too many records; cache unchanged");
                }
                String time=new SimpleDateFormat("dd MMM yyyy, hh:mm a",Locale.getDefault()).format(new Date());
                prefs.edit().putString("cache",all.toString()).putString("last_sync",time).apply();records=all;
                runOnUiThread(()->{loading=false;showHome();toast("Updated "+records.length()+" active records");});
            }catch(Exception ex){
                runOnUiThread(()->{loading=false;toast("Sync unavailable; offline records kept: "+ex.getMessage());});
            }
        });
    }
    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
