import sys
import json
import traceback
import pymannkendall as mk

if __name__ == "__main__":
    try:
        values = json.loads(sys.argv[1])

        if not isinstance(values, list) or len(values) < 3:
            print("Input must be a list with at least 3 numeric values.", file=sys.stderr)
            sys.exit(1)

        if any(not isinstance(x, (int, float)) for x in values):
            print("All input values must be numeric.", file=sys.stderr)
            sys.exit(1)

        result = mk.original_test(values)

        output = {
            "trend": str(result.trend),
            "h": bool(result.h),
            "p": float(result.p),
            "z": float(result.z),
            "Tau": float(result.Tau),
            "s": int(result.s),
            "var_s": float(result.var_s),
            "slope": float(result.slope),
            "intercept": float(result.intercept)
        }

        print(json.dumps(output))

    except Exception as e:
        print(f"Exception in Mann-Kendall script: {e}", file=sys.stderr)
        traceback.print_exc(file=sys.stderr)
        sys.exit(1)
