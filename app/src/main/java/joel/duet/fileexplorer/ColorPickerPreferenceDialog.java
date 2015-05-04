package joel.duet.fileexplorer;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class ColorPickerPreferenceDialog extends DialogPreference implements ColorPickerView.OnColorChangedListener {
	private int mColor = 0;

	public ColorPickerPreferenceDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * D�clench� d�s qu'on ferme la bo�te de dialogue
	 */
	protected void onDialogClosed(boolean positiveResult) {
		// Si l'utilisateur a cliqu� sur "OK"
		if (positiveResult) {
			persistInt(mColor);
			// OU getSharedPreferences().edit().putInt(getKey(), mColor).commit();
		}

		super.onDialogClosed(positiveResult);
	}

	/**
	 * Pour construire la bo�te de dialogue
	 */
	protected void onPrepareDialogBuilder(Builder builder) {
		// On r�cup�re l'ancienne couleur ou la couleur par d�faut
		int oldColor = getSharedPreferences().getInt(getKey(), Color.BLACK);
		// On ins�re la vue dans la bo�te de dialogue
		builder.setView(new ColorPickerView(getContext(), this, oldColor));

		super.onPrepareDialogBuilder(builder);
	}

	/**
	 * D�clench� � chaque fois que l'utilisateur choisit une couleur
	 */
	public void colorChanged(int color) {
		mColor = color;
	}
}