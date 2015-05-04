package joel.duet.fileexplorer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ExplorateurActivity extends ListActivity implements OnSharedPreferenceChangeListener {
    /**
     * Notre Adapter personnalisé qui lie les fichiers à la liste
     */
    private FileAdapter mAdapter = null;

    /**
     * Représente le répertoire actuel
     */
    private File mCurrentFile = null;
    /**
     * Couleur voulue pour les répertoires
     */
    private int mColor = 0;
    /**
     * Indique si l'utilisateur est à la racine ou pas
     * pour savoir s'il veut quitter
     */
    private boolean mCountdown = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorateur);

        // On récupère la ListView de notre activité
        /*
        La liste qui contient nos fichiers et répertoires
        */
        ListView mList = getListView();

        // On vérifie que le répertoire externe est bien accessible
        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // S'il ne l'est pas, on affiche un message
            /*
            Représente le texte qui s'affiche quand la liste est vide
            */
            TextView mEmpty = (TextView) mList.getEmptyView();
            mEmpty.setText("Vous ne pouvez pas accéder aux fichiers");
        } else {
            // S'il l'est...
            // On déclare qu'on veut un menu contextuel sur les éléments de la liste
            registerForContextMenu(mList);

            // On récupère les préférences de l'application
            /*
            Les préférences partagées de cette application
            */
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            // On indique que l'acitivté est à l'écoute des changements de préférence
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            // On récupère la couleur voulue par l'utilisateur, par défaut il s'agira du rouge
            mColor = mPrefs.getInt("repertoireColorPref", Color.RED);

            // On récupère la racine de la carte SD pour qu'elle soit
            mCurrentFile = Environment.getExternalStorageDirectory();

            // On change le titre de l'activité pour y mettre le chemin actuel
            setTitle(mCurrentFile.getAbsolutePath());

            // On récupère la liste des fichiers dans le répertoire actuel
            File[] fichiers = mCurrentFile.listFiles();

            // On transforme le tableau en une structure de données de taille variable
            ArrayList<File> liste = new ArrayList<>();
            Collections.addAll(liste, fichiers);

            mAdapter = new FileAdapter(this, liste);
            // On ajoute l'adaptateur à la liste
            mList.setAdapter(mAdapter);
            // On trie la liste
            mAdapter.sort();

            // On ajoute un Listener sur les items de la liste
            mList.setOnItemClickListener(new OnItemClickListener() {

                // Que se passe-il en cas de cas de clic sur un élément de la liste ?
                public void onItemClick(AdapterView<?> adapter, View view,
                                        int position, long id) {
                    File fichier = mAdapter.getItem(position);
                    // Si le fichier est un répertoire...
                    if (fichier.isDirectory())
                        // On change de répertoire courant
                        updateDirectory(fichier);
                    else
                        // Sinon on lance l'irzm
                        seeItem(fichier);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_explorateur, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_options:
                // Intent explicite
                Intent i = new Intent(this, ExploreurPreferenceWithHeader.class);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View vue,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, vue, menuInfo);

        MenuInflater inflater = getMenuInflater();
        // On récupère des informations sur l'item par apport à l'Adapter
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;

        // On récupère le fichier conserné par le menu contextuel
        File fichier = mAdapter.getItem(info.position);
        // On a deux menus, en fonction qu'il s'agit d'un répertoire ou d'un fichier
        if(!fichier.isDirectory())
            inflater.inflate(R.menu.context_file, menu);
        else
            inflater.inflate(R.menu.context_dir, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        // On récupère la position de l'item concerné
        File fichier = mAdapter.getItem(info.position);
        switch (item.getItemId()) {
            case R.id.deleteItem:
                mAdapter.remove(fichier);
                return fichier.delete();

            case R.id.seeItem:
                seeItem(fichier);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Utilisé pour visualiser un fichier
     * @param pFile le fichier à visualiser
     */
    private void seeItem(File pFile) {
        // On créé un Intent
        Intent i = new Intent(Intent.ACTION_VIEW);

        // On détermine son type MIME
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String ext = pFile.getName().substring(pFile.getName().indexOf(".") + 1).toLowerCase();
        String type = mime.getMimeTypeFromExtension(ext);

        // On ajoute les informations nécessaires
        i.setDataAndType(Uri.fromFile(pFile), type);

        try {
            startActivity(i);
            // Et s'il n'y a pas d'activité qui puisse gérer ce type de fichier
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Oups, vous n'avez pas d'application qui puisse lancer ce fichier", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Utilisé pour naviguer entre les répertoires
     * @param pFile le nouveau répertoire dans lequel aller
     */

    private void updateDirectory(File pFile) {
        // On change le titre de l'activité
        setTitle(pFile.getAbsolutePath());

        // L'utilisateur ne souhaite plus sortir de l'application
        mCountdown = false;

        // On change le repertoire actuel
        mCurrentFile = pFile;
        // On vide les répertoires actuels
        setEmpty();

        // On récupère la liste des fichiers du nouveau répertoire
        File[] fichiers = mCurrentFile.listFiles();

        // Si le répertoire n'est pas vide...
        if(fichiers != null)
            // On les ajoute à  l'adaptateur
            for(File f : fichiers)
                mAdapter.add(f);
        // Et on trie l'adaptateur
        mAdapter.sort();
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        // Si on a appuyé sur le retour arrière
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            // On prend le parent du répertoire courant
            File parent = mCurrentFile.getParentFile();
            // S'il y a effectivement un parent
            if(parent != null)
                updateDirectory(parent);
            else {
                // Sinon, si c'est la première fois qu'on fait un retour arrière
                if(!mCountdown) {
                    // On indique à l'utilisateur qu'appuyer dessus une seconde fois le fera sortir
                    Toast.makeText(this, "Nous sommes déjà à la racine ! Cliquez une seconde fois pour quitter", Toast.LENGTH_SHORT).show();
                    mCountdown  = true;
                } else
                    // Si c'est la seconde fois on sort effectivement
                    finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * On enlève tous les éléments de la liste
     */

    private void setEmpty() {
        // Si l'adapteur n'est pas vide...
        if(!mAdapter.isEmpty())
            // Alors on le vide !
            mAdapter.clear();
    }

    /**
     * L'adaptateur spécifique à nos fichiers
     */

    private class FileAdapter extends ArrayAdapter<File> {
        /**
         * Permet de comparer deux fichiers
         *
         */
        private class FileComparator implements Comparator<File> {

            public int compare(File lhs, File rhs) {
                // si lhs est un répertoire et pas l'autre, il est plus petit
                if(lhs.isDirectory() && rhs.isFile())
                    return -1;
                // dans le cas inverse, il est plus grand
                if(lhs.isFile() && rhs.isDirectory())
                    return 1;

                //Enfin on ordonne en fonction de l'ordre alphabétique sans tenir compte de la casse
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }

        }

        public FileAdapter(Context context,
                           List<File> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
            mInflater = LayoutInflater.from(context);
        }

        private LayoutInflater mInflater = null;

        /**
         * Construit la vue en fonction de l'item
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView vue;

            if(convertView != null)
                vue = (TextView) convertView;
            else
                vue = (TextView) mInflater.inflate(android.R.layout.simple_list_item_1, null);
            File item = getItem(position);
            //Si c'est un répertoire, on choisit la couleur dans les préférences
            if(item.isDirectory())
                vue.setTextColor(mColor);
            else
                // Sinon c'est du noir
                vue.setTextColor(Color.BLACK);
            vue.setText(item.getName());
            return vue;
        }

        /**
         * Pour trier rapidement les éléments de l'adaptateur
         */
        public void sort () {
            super.sort(new FileComparator());
        }
    }

    /**
     * Se déclenche dès qu'une préférence a changé
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        mColor = sharedPreferences.getInt("repertoireColorPref", Color.BLACK);
        mAdapter.notifyDataSetInvalidated();
    }
}


