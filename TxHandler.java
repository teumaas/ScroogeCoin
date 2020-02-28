import java.util.*;

public class TxHandler {
	// UTXOPool heeft een instannt van de TX

	// De UTXOPool wordt hier aangemaakt waar de TxHandler mee zal werken tijdens de uitvoer van deze klasse.
	// Als voorbeeld heeft de TXPool alle geverifieerde outputs van de previous transacties die geldig zijn in de pool.
	// Bij uitgave van een coin zal deze weggaan als saldo binnen de UTXOPool.
	private UTXOPool UTXOPool;

	/* Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is utxoPool. This should make a defensive copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */

	// Hier wordt de UTXOPool geïnitialiseerd met een nieuwe UXTOPool van uit de parameters meegegeven wordt.
	public TxHandler(UTXOPool utxoPool) {
		this.UTXOPool = new UTXOPool(utxoPool);
	}

	/* Returns true if
	 * (1) all outputs claimed by tx are in the current UTXO pool,
	 * (2) the signatures on each input of tx are valid,
	 * (3) no UTXO is claimed multiple times by tx,
	 * (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of
	        its output values;
	   and false otherwise.
	 */

	// Dit is de validatie functie waarbij per een transactie zal worden gevalideerd.
	// Hier zal dus met een foreach loop door meerdere transacties worden doorlopen.
	public boolean isValidTx(Transaction tx) {

		// Hier wordt een nieuwe HashSet gemaakt met de gebruikte UTXO's.
		HashSet<UTXO> UTXOUsed = new HashSet<UTXO>();

		// Hier wordt een basis gelegd met initialisatie van de valaues i/o en het transactie Index die erbij hoort.
		double valueInput = 0.0;
		double valueOutput = 0.0;
		int transIndex = 0;

		// Dit een loop die door elke transactie heen loopt.
		for (int i = 0; i < tx.getInputs().size(); i++) {
			Transaction.Input input = tx.getInput(i);
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

			// Er wordt gecontrolleerd of een transactie ook daadwerkelijk bestaat.
			if (!this.UTXOPool.contains(utxo)) {
				return false;
			}

			//Het controlleren of de signature dadwerkelijk geldig is verklaard binnen de UTXOPool daarbij of de afzender wel goedgekeurd heeft.
			RSAKey rsaKey = UTXOPool.getTxOutput(utxo).address;

			if (!rsaKey.verifySignature(tx.getRawDataToSign(i), input.signature)){
				return false;
			}


			// Checkt of de UTXO daadwerkelijk maar één keer gebruikt is. (Voorkomt double spending attacks)
			if (UTXOUsed.contains(utxo)) {
				return false;
			}

			// Voegt de transactie toe, aan de HashSet vorige transactie Hash en de output index.
			UTXOUsed.add(utxo);

			// Voegt de input waarde toe aan de totale som van alle gemaakte inputs.
			valueInput += UTXOPool.getTxOutput(utxo).value;
		}

		// Door loopt alle outputs
		for (Transaction.Output out : tx.getOutputs()) {
			// Controlleert of de waarde van de output niet in het min staat.
			if (out.value < 0.0) {
				return false;
			}

			// Vervolgens zal de output waarden worden toegevoegd.
			valueOutput += out.value;
		}

		// Controleert of de waarde van inputs groter is of gelijk aan de waarde van de output value
		if (valueOutput >= valueInput) {
			return false;
		}

		// Na het doorlopen van al deze stapen zal er uit eindelijk een true worden terggegeven.
		return true;
	}

	/* Handles each epoch by receiving an unordered array of proposed
	 * transactions, checking each transaction for correctness,
	 * returning a mutually valid array of accepted transactions,
	 * and updating the current UTXO pool as appropriate.
	 */

	// Functie voor het verwerken van meerdere transacties.
	public Transaction[] handleTxs(Transaction[] possibleTxs) {

		//De gevalideerde transacties worden opgeslagen in een tijdelijke ArrayList.
		List<Transaction> transactionValid = new ArrayList<>();

		//De transacties in de lijst zullen worden afgegaan in volgorde.
		for (Transaction transaction : possibleTxs){
			if (isValidTx(transaction)){
				int i = 0;

				//De gemaakte inputs van de transactie lijst zullen worden verwijderd uit de UTXOPool
				for (Transaction.Input input : transaction.getInputs()){
					UTXO UTXO = new UTXO(input.prevTxHash, input.outputIndex);
					this.UTXOPool.removeUTXO(UTXO);
				}

				//De outputs van de huidige transactie worden toegevoegd aan de UTXO pool
				for (Transaction.Output output : transaction.getOutputs()){
					UTXO UTXO = new UTXO(transaction.getHash(), i);
					UTXOPool.addUTXO(UTXO, output);

					i++;
				}

				//Nu worden geldige transacties toegevoegd aan de lijst met transacties.
				transactionValid.add(transaction);
			}
		}

		// De transacties die zijn goedgekeurd worden geconvert naar een array.
		Transaction[] transactions = new Transaction[transactionValid.size()];
		transactionValid.toArray(transactions);

		// Vervolgens zal de lijst met transacties worden gereturned.
		return transactions;
	}
} 
