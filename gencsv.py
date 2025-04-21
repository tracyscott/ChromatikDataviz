import random
import csv
import sys
import argparse

def generate_random_csv(rows, cols, output_file='random_data.csv'):
    # Write to CSV file
    with open(output_file, 'w', newline='') as f:
        writer = csv.writer(f)
        for _ in range(rows):
            # Generate one row at a time
            row = [round(random.random(), 1) for _ in range(cols)]
            writer.writerow(row)

def main():
    # Set up argument parser
    parser = argparse.ArgumentParser(description='Generate CSV with random values between 0.0 and 1.0')
    parser.add_argument('rows', type=int, help='Number of rows')
    parser.add_argument('cols', type=int, help='Number of columns')
    parser.add_argument('--output', '-o', default='random_data.csv', 
                        help='Output file name (default: random_data.csv)')

    # Parse arguments
    args = parser.parse_args()

    # Generate the CSV
    generate_random_csv(args.rows, args.cols, args.output)
    print(f"Generated CSV file with {args.rows} rows and {args.cols} columns")

if __name__ == "__main__":
    main()