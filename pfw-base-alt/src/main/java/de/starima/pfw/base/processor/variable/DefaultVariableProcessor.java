package de.starima.pfw.base.processor.variable;

import de.dzbank.recon.components.base.utils.ReconManagerHelper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.attribute.api.IAttributeProviderProcessor;
import de.starima.pfw.base.processor.attribute.domain.RcnAttribute;
import de.starima.pfw.base.processor.variable.api.IVariableProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;

@Slf4j
@Getter @Setter
@Processor
public class DefaultVariableProcessor extends AbstractProcessor implements IVariableProcessor {

	@ProcessorParameter
	protected IAttributeProviderProcessor<IAttribute> attributeProcessor;

	// cache
	private HashMap<String, IAttribute> variableMap = new HashMap<>();

	@Override
	public IAttribute getSystemVariable(String name) throws Exception {
		if (log.isDebugEnabled())
			log.debug("{}.getSystemVariable({})", this.getIdentifier(), name);
		if (name == null)
			return null;
		IAttribute vd;

		// Operator, der im Namen der Systemvariable enthalten sein kann
		String sysVarOperator = null;
		// Operant (Integer), der im Namen der Systemvariable enthalten sein
		// kann
		String sysVarOperant = null;

		// zerlege den Namen nach den Pattern _PLUS_ oder _MINUS_
		String[] nameArr = name.split("_PLUS_");
		if (nameArr.length == 2) {
			sysVarOperator = "PLUS";
			sysVarOperant = nameArr[1];
		} else {
			nameArr = name.split("_MINUS_");
			if (nameArr.length == 2) {
				sysVarOperator = "MINUS";
				sysVarOperant = nameArr[1];
			}
		}

		String varName = nameArr[0];

		switch (varName.toUpperCase()) {
			case ReconManagerHelper.ATTRIBUTE_VALUATIONDATE:
				vd = variableMap.get(name);
				if (vd == null) {
					vd = createValuationDateVariable(sysVarOperator, sysVarOperant);
					variableMap.put(name, vd);
				}
				return vd;
			case ReconManagerHelper.ATTRIBUTE_VALUATIONDATE_AS_STRING:
				vd = variableMap.get(name);
				if (vd == null) {
					vd = createValuationDateAsStringVariable(sysVarOperator, sysVarOperant);
					variableMap.put(name, vd);
				}
				return vd;
			default:
				return null;
		}
	}

	@Override
	public IAttribute getUserVariable(String name) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	private Date createValuationDate(String sysVarOperator, String sysVarOperant) {
		if (this.reconContext instanceof ReconProcessorContext) {
			ReconResult result = ((ReconProcessorContext)this.reconContext).getReconResult();
			if (result != null) {
				String classifier = result.getResultClassifier();
				try {
					// default method uses the classical valuation date
					SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
					classifier = StringUtils.commaDelimitedListToStringArray(classifier)[0];
					Date valDate = df.parse(classifier);

					try {
						if ("PLUS".equals(sysVarOperator)) {							
							valDate = Date.from(
										df.parse(classifier).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
										.plusDays(Integer.parseInt(sysVarOperant))
										.atStartOfDay(ZoneId.systemDefault()).toInstant()
									);
						} else if ("MINUS".equals(sysVarOperator)) {
							valDate = Date.from(
										df.parse(classifier).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
										.minusDays(Integer.parseInt(sysVarOperant))
										.atStartOfDay(ZoneId.systemDefault()).toInstant()
									);
						}
					} catch (Exception e) {

					}

					if (log.isDebugEnabled())
						log.debug(String.format(
								"Success create valuation date! classifier=%s, sysVarOperator=%s, sysVarOperant=%s, finalValue=%s",
								classifier, sysVarOperator, sysVarOperant,
								df.format(valDate)));

					return valDate;
				} catch (Exception e) {
					return null;
				}

			}
		}
		return null;
	}

	private IAttribute createValuationDateVariable(String sysVarOperator, String sysVarOperant) {
		Date valDate = createValuationDate(sysVarOperator, sysVarOperant);
		IAttribute vd = attributeProcessor.getAttribute(ReconManagerHelper.ATTRIBUTE_VALUATIONDATE);
		if (vd != null) {
			vd.setValue(valDate);

			if (log.isDebugEnabled())
				log.debug(String.format("Success create variable %s!", ReconManagerHelper.ATTRIBUTE_VALUATIONDATE));
		}
		return vd;
	}

	private IAttribute createValuationDateAsStringVariable(String sysVarOperator, String sysVarOperant) {
		Date valDate = createValuationDate(sysVarOperator, sysVarOperant);
		SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd");

		IAttribute vd = new RcnAttribute();
		vd.setName(ReconManagerHelper.ATTRIBUTE_VALUATIONDATE_AS_STRING);
		vd.setType("char");
		vd.setValue(df.format(valDate));
		log.debug("Success create variable {} with formatted value {}!",
				ReconManagerHelper.ATTRIBUTE_VALUATIONDATE, vd.getValue());

		return vd;
	}
}