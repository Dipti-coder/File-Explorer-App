package com.example.fileexplorer.Fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fileexplorer.FileAdapter;
import com.example.fileexplorer.FileOpener;
import com.example.fileexplorer.OnFileSelectedListener;
import com.example.fileexplorer.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InternalFragment extends Fragment implements OnFileSelectedListener {

    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private List<File> filelist;
    private ImageView img_back;
    private TextView tv_pathHolder;
    File storage;
    String data;
    String[] items = {"Details","Rename","Delete","Share"};
    View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_internal,container,false);

        tv_pathHolder = view.findViewById(R.id.tv_pathHolder);
        img_back = view.findViewById(R.id.img_back);



        String internalStorage = System.getenv("EXTERNAL_STORAGE");
        storage = new File(internalStorage);

        try {
            data = getArguments().getString("path");
            File file = new File(data);
            storage = file;
        }catch (Exception e){
            e.printStackTrace();
        }
        tv_pathHolder.setText(storage.getAbsolutePath());
        runtimePermission();
        return view;
    }

    private void runtimePermission() {
     Dexter.withContext(getContext()).withPermissions(
             Manifest.permission.WRITE_EXTERNAL_STORAGE,
             Manifest.permission.READ_EXTERNAL_STORAGE
     ).withListener(new MultiplePermissionsListener() {
         @Override
         public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
             displayFiles();
         }

         @Override
         public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {

             permissionToken.continuePermissionRequest();
         }
     }).check();
    }

    public ArrayList<File> findFiles (File file){
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();

        for (File singleFile: files){
             if (singleFile.isDirectory() && !singleFile.isHidden()){
                 arrayList.add(singleFile);
             }
        }

        for (File singleFile: files){
            if (singleFile.getName().toLowerCase().endsWith(".jpeg") || singleFile.getName().toLowerCase().endsWith(".jpg") ||
                    singleFile.getName().toLowerCase().endsWith(".png") || singleFile.getName().toLowerCase().endsWith(".wav") ||
                    singleFile.getName().toLowerCase().endsWith(".pdf") || singleFile.getName().toLowerCase().endsWith(".apk") ||
                    singleFile.getName().toLowerCase().endsWith(".mp3") || singleFile.getName().toLowerCase().endsWith(".mp4") ||
                    singleFile.getName().toLowerCase().endsWith(".doc")){

                arrayList.add(singleFile);
            }
        }
        return arrayList;
    }
    private void displayFiles() {

        recyclerView = view.findViewById(R.id.recycler_internal);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(),2));
        filelist = new ArrayList<>();
        filelist.addAll(findFiles(storage));
        fileAdapter = new FileAdapter(getContext(),filelist,this);
        recyclerView.setAdapter(fileAdapter);

    }

    @Override
    public void onFileClicked(File file) {
        if (file.isDirectory()){
            Bundle bundle = new Bundle();
            bundle.putString("path",file.getAbsolutePath());
            InternalFragment internalFragment = new InternalFragment();
            internalFragment.setArguments(bundle);
            getFragmentManager().beginTransaction().replace(R.id.fragment_container,internalFragment).addToBackStack(null).commit();
        }else {

            try {
                FileOpener.openFile(getContext(),file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onFileLongClicked(File file,int position) {
        final Dialog optionDialog = new Dialog(getContext());
        optionDialog.setContentView(R.layout.option_dialog);
        optionDialog.setTitle("Select Option.");
        ListView option = (ListView) optionDialog.findViewById(R.id.list);
        CustomAdapter customAdapter = new CustomAdapter();
        option.setAdapter(customAdapter);
        optionDialog.show();


        option.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();

                switch (selectedItem){
                    case "Details":
                        AlertDialog.Builder detailDialog = new AlertDialog.Builder(getContext());
                        detailDialog.setTitle("Details:");
                        final TextView details = new TextView(getContext());
                        detailDialog.setView(details);

                        Date lastModified = new Date(file.lastModified());
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy  HH:mm:ss");
                        String formattedDate = formatter.format(lastModified);

                        details.setText("File Name: " + file.getName() + "\n" +
                                "Size: "+ Formatter.formatShortFileSize(getContext(),file.length()) + "\n" +
                                "Path: "+ file.getAbsolutePath() + "\n" +
                                "Last Modified : " + formattedDate);
                        detailDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                optionDialog.cancel();
                            }
                        });

                        AlertDialog alertDialog_details = detailDialog.create();
                        alertDialog_details.show();
                        break;
                    case "Rename":

                        AlertDialog.Builder renameDialog = new AlertDialog.Builder(getContext());
                        renameDialog.setTitle("Rename File: ");
                        final EditText names = new EditText(getContext());
                        renameDialog.setView(names);
                        renameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String new_name = names.getEditableText().toString();
                                String extension = "";
                                if (!file.isDirectory()){
                                    extension = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
                                }
                                File current = new File(file.getAbsolutePath());
                                File destination = new File(file.getAbsolutePath().replace(file.getName(),new_name + extension));
                                if (current.renameTo(destination)){
                                    filelist.set(position,destination);
                                    fileAdapter.notifyItemChanged(position);
                                    Toast.makeText(getContext(), "Renamed!", Toast.LENGTH_SHORT).show();
                                }else {
                                    Toast.makeText(getContext(), "Couldn't Rename!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        renameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                optionDialog.cancel();
                            }
                        });
                        AlertDialog alertDialog_rename = renameDialog.create();
                        alertDialog_rename.show();
                        break;

                    case "Delete":
                        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(getContext());
                        deleteDialog.setTitle("Delete" + file.getName() + "?");
                        deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                file.delete();
                                filelist.remove(position);
                                fileAdapter.notifyDataSetChanged();
                                Toast.makeText(getContext(), "Deleted!", Toast.LENGTH_SHORT).show();

                            }
                        });

                        deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                optionDialog.cancel();
                            }
                        });

                        AlertDialog alertDialog_delete = deleteDialog.create();
                        alertDialog_delete.show();
                        break;

                    case "Share":
                        String fileName = file.getName();
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("image/jpeg");

                        Uri contentUri = FileProvider.getUriForFile(getContext(), "com.example.fileexplorer.provider", file);
                        share.putExtra(Intent.EXTRA_STREAM, contentUri);

                        Intent chooser = Intent.createChooser(share, "Share " + fileName);
                        List<ResolveInfo> resInfoList = getContext().getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);

                        for (ResolveInfo resolveInfo : resInfoList) {
                            String packageName = resolveInfo.activityInfo.packageName;
                            getContext().grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }

                        startActivity(chooser);
                        break;

//                    case "Share":
//                        String fileName = file.getName();
//                        Intent share = new Intent();
//                        share.setAction(Intent.ACTION_SEND);
//
//                        // Determine the MIME type based on the file type
//                        String mimeType;
//                        if (file.isDirectory()) {
//                            mimeType = "*/*"; // For directories, use any MIME type
//                        } else {
//                            // Determine the MIME type based on the file extension
//                            String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
//                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
//                            if (mimeType == null) {
//                                mimeType = "*/*"; // If MIME type cannot be determined, use any MIME type
//                            }
//                        }
//
//                        share.setType(mimeType);
//                        share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
//                        startActivity(Intent.createChooser(share, "Share" + fileName));
//                        break;

                }
            }
        });
    }

    class CustomAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Object getItem(int position) {
            return items[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View myView = getLayoutInflater().inflate(R.layout.option_layout,null);
            TextView txtOptions = myView.findViewById(R.id.txtOption);
            ImageView  imgOptions = myView.findViewById(R.id.imgOption);

            txtOptions.setText(items[position]);
            if (items[position].equals("Details")){
                imgOptions.setImageResource(R.drawable.ic_details);
            }else if (items[position].equals("Rename")){
                imgOptions.setImageResource(R.drawable.ic_rename);
            }else if (items[position].equals("Delete")){
                imgOptions.setImageResource(R.drawable.ic_delete);
            } else if (items[position].equals("Share")) {
                imgOptions.setImageResource(R.drawable.ic_share);

            }

            return myView;
        }
    }
}
